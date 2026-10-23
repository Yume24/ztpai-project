package com.rendezvous.room.service;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomPermissions;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.dto.MemberResponse;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.room.event.MemberJoinedEvent;
import com.rendezvous.room.event.MemberLeftEvent;
import com.rendezvous.room.event.MemberRemovedEvent;
import com.rendezvous.room.event.MemberRoleChangedEvent;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomMembershipService {

    static final int MAX_ACTIVE_MEMBERS = 50;

    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final UserRepository users;
    private final RoomAccessService access;
    private final RoomDetailsAssembler assembler;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RoomMembershipService(RoomRepository rooms, RoomMemberRepository members, UserRepository users,
                                 RoomAccessService access, RoomDetailsAssembler assembler,
                                 ApplicationEventPublisher events, Clock clock) {
        this.rooms = rooms;
        this.members = members;
        this.users = users;
        this.access = access;
        this.assembler = assembler;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public RoomDetailsResponse join(UUID userId, String code) {
        Instant now = clock.instant();
        Room room = rooms.findByCodeForUpdate(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND));
        if (room.statusAt(now) == RoomStatus.ENDED) {
            throw new ApiException(ErrorCode.ROOM_ENDED);
        }
        Optional<RoomMember> existing = members.findByRoomIdAndUserId(room.getId(), userId);
        RoomMember membership;
        if (existing.isPresent() && existing.get().getStatus() == MemberStatus.ACTIVE) {
            return assembler.toDetails(room, existing.get());
        } else if (existing.isPresent() && existing.get().getStatus() == MemberStatus.REMOVED) {
            throw new ApiException(ErrorCode.MEMBER_REMOVED);
        } else if (existing.isPresent()) {
            requireFreeSlot(room);
            membership = existing.get();
            membership.rejoin(now);
        } else {
            requireFreeSlot(room);
            User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
            membership = members.save(new RoomMember(room, user, RoomRole.MEMBER, now));
        }
        events.publishEvent(new MemberJoinedEvent(room.getId(), userId));
        return assembler.toDetails(room, membership);
    }

    @Transactional
    public void leave(UUID roomId, UUID userId) {
        MemberContext context = access.requireMember(roomId, userId);
        if (!RoomPermissions.canLeave(context.role())) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        Instant now = clock.instant();
        context.member().leave(now);
        members.save(context.member());
        boolean roomEnded = context.room().statusAt(now) == RoomStatus.ENDED;
        events.publishEvent(new MemberLeftEvent(roomId, userId, roomEnded));
    }

    @Transactional
    public void remove(UUID roomId, UUID actorId, UUID targetUserId) {
        MemberContext actor = access.requireMember(roomId, actorId);
        Instant now = clock.instant();
        requireNotEnded(actor.room(), now);
        RoomMember target = requireActiveTarget(roomId, targetUserId);
        if (!RoomPermissions.canRemove(actor.role(), target.getRole())) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        removeMember(target, now);
    }

    @Transactional
    public MemberResponse changeRole(UUID roomId, UUID actorId, UUID targetUserId, RoomRole newRole) {
        MemberContext actor = access.requireMember(roomId, actorId);
        if (!RoomPermissions.canChangeRoles(actor.role())) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        if (newRole == RoomRole.OWNER) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Ownership cannot be assigned.");
        }
        requireNotEnded(actor.room(), clock.instant());
        RoomMember target = requireActiveTarget(roomId, targetUserId);
        if (target.getRole() == RoomRole.OWNER) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        target.changeRole(newRole);
        members.save(target);
        events.publishEvent(new MemberRoleChangedEvent(roomId, targetUserId));
        return MemberResponse.from(target);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> members(UUID roomId, UUID userId) {
        access.requireMember(roomId, userId);
        return members.findByRoomIdAndStatus(roomId, MemberStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(RoomMember::getJoinedAt))
                .map(MemberResponse::from)
                .toList();
    }

    @Transactional
    public void removeFromOpenRooms(UUID userId) {
        Instant now = clock.instant();
        members.findActiveInOpenRooms(userId, now).stream()
                .filter(member -> member.getRole() != RoomRole.OWNER)
                .forEach(member -> removeMember(member, now));
    }

    private void removeMember(RoomMember target, Instant now) {
        target.remove(now);
        members.save(target);
        events.publishEvent(new MemberRemovedEvent(target.getRoom().getId(), target.getRoom().getName(),
                target.getUser().getId()));
    }

    private RoomMember requireActiveTarget(UUID roomId, UUID targetUserId) {
        return members.findByRoomIdAndUserId(roomId, targetUserId)
                .filter(found -> found.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void requireNotEnded(Room room, Instant now) {
        if (room.statusAt(now) == RoomStatus.ENDED) {
            throw new ApiException(ErrorCode.ROOM_ENDED);
        }
    }

    private void requireFreeSlot(Room room) {
        if (members.countByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE) >= MAX_ACTIVE_MEMBERS) {
            throw new ApiException(ErrorCode.ROOM_FULL);
        }
    }
}
