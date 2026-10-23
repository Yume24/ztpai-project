package com.rendezvous.room.service;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.common.web.PageResponse;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomPermissions;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.domain.RoomTimeWindowPolicy;
import com.rendezvous.room.dto.CreateRoomRequest;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.room.dto.RoomSummaryResponse;
import com.rendezvous.room.dto.UpdateRoomRequest;
import com.rendezvous.room.event.RoomUpdatedEvent;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.room.repository.RoomSpecifications;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final UserRepository users;
    private final RoomCodeGenerator codes;
    private final RoomAccessService access;
    private final RoomDetailsAssembler assembler;
    private final RoomLifecycleService lifecycle;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RoomService(RoomRepository rooms, RoomMemberRepository members, UserRepository users,
                       RoomCodeGenerator codes, RoomAccessService access, RoomDetailsAssembler assembler,
                       RoomLifecycleService lifecycle, ApplicationEventPublisher events, Clock clock) {
        this.rooms = rooms;
        this.members = members;
        this.users = users;
        this.codes = codes;
        this.access = access;
        this.assembler = assembler;
        this.lifecycle = lifecycle;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public RoomDetailsResponse create(UUID userId, CreateRoomRequest request) {
        Instant now = clock.instant();
        Instant startsAt = request.startsAt() != null ? request.startsAt() : now;
        RoomTimeWindowPolicy.validateNew(startsAt, request.endsAt(), now);

        User owner = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Room room = new Room(request.name(), uniqueCode(), owner, startsAt, request.endsAt(), now);
        if (!startsAt.isAfter(now)) {
            room.markStartNotified(now);
        }
        rooms.saveAndFlush(room);
        RoomMember membership = new RoomMember(room, owner, RoomRole.OWNER, now);
        members.save(membership);
        return assembler.toDetails(room, membership);
    }

    @Transactional(readOnly = true)
    public RoomDetailsResponse get(UUID roomId, UUID userId) {
        MemberContext context = access.requireMember(roomId, userId);
        return assembler.toDetails(context.room(), context.member());
    }

    @Transactional
    public RoomDetailsResponse update(UUID roomId, UUID userId, UpdateRoomRequest request) {
        MemberContext context = access.requireMember(roomId, userId);
        Room room = context.room();
        if (!RoomPermissions.canEditRoom(context.role())) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        Instant now = clock.instant();
        if (room.statusAt(now) == RoomStatus.ENDED) {
            throw new ApiException(ErrorCode.ROOM_ENDED);
        }
        if (!request.version().equals(room.getVersion())) {
            throw new ApiException(ErrorCode.ROOM_VERSION_CONFLICT);
        }
        applyChanges(room, request, now);
        rooms.saveAndFlush(room);
        events.publishEvent(new RoomUpdatedEvent(room.getId()));
        return assembler.toDetails(room, context.member());
    }

    @Transactional
    public void close(UUID roomId, UUID userId) {
        lifecycle.close(requireCloseOrDeleteRights(roomId, userId));
    }

    @Transactional
    public void delete(UUID roomId, UUID userId) {
        Room room = requireCloseOrDeleteRights(roomId, userId);
        if (room.statusAt(clock.instant()) == RoomStatus.ENDED) {
            lifecycle.processEnd(room);
        } else {
            lifecycle.close(room);
        }
        rooms.delete(room);
        rooms.flush();
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomSummaryResponse> myRooms(UUID userId, RoomStatus status, Pageable pageable) {
        Instant now = clock.instant();
        Specification<Room> spec = RoomSpecifications.hasActiveMember(userId)
                .and(RoomSpecifications.hasStatus(status, now));
        return PageResponse.from(rooms.findAll(spec, pageable).map(room -> summarize(room, userId, now)));
    }

    private Room requireCloseOrDeleteRights(UUID roomId, UUID userId) {
        MemberContext context = access.requireMember(roomId, userId);
        if (!RoomPermissions.canCloseOrDelete(context.role())) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        return context.room();
    }

    private void applyChanges(Room room, UpdateRoomRequest request, Instant now) {
        if (request.name() != null) {
            room.rename(request.name());
        }
        if (request.startsAt() != null || request.endsAt() != null) {
            Instant startsAt = request.startsAt() != null ? request.startsAt() : room.getStartsAt();
            Instant endsAt = request.endsAt() != null ? request.endsAt() : room.getEndsAt();
            RoomTimeWindowPolicy.validateChange(room, startsAt, endsAt, now);
            room.reschedule(startsAt, endsAt);
        }
        if (request.visibilityMode() != null || request.membersSeeTrails() != null) {
            room.changeSettings(
                    request.visibilityMode() != null ? request.visibilityMode() : room.getVisibilityMode(),
                    request.membersSeeTrails() != null ? request.membersSeeTrails() : room.isMembersSeeTrails());
        }
    }

    private RoomSummaryResponse summarize(Room room, UUID userId, Instant now) {
        RoomRole role = members.findByRoomIdAndUserId(room.getId(), userId).orElseThrow().getRole();
        long memberCount = members.countByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE);
        return new RoomSummaryResponse(room.getId(), room.getName(), room.statusAt(now), room.getStartsAt(),
                room.getEndsAt(), role, memberCount);
    }

    private String uniqueCode() {
        String code;
        do {
            code = codes.generate();
        } while (rooms.existsByCode(code));
        return code;
    }
}
