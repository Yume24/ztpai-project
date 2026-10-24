package com.rendezvous.admin.service;

import com.rendezvous.admin.dto.AdminRoomResponse;
import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.common.web.PageResponse;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.room.repository.RoomSpecifications;
import com.rendezvous.room.service.RoomLifecycleService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRoomService {

    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final RoomLifecycleService lifecycle;
    private final Clock clock;

    public AdminRoomService(RoomRepository rooms, RoomMemberRepository members, RoomLifecycleService lifecycle,
                            Clock clock) {
        this.rooms = rooms;
        this.members = members;
        this.lifecycle = lifecycle;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminRoomResponse> search(RoomStatus status, Instant from, Instant to, Pageable pageable) {
        Instant now = clock.instant();
        return PageResponse.from(rooms.findAll(
                RoomSpecifications.hasStatus(status, now).and(RoomSpecifications.startsBetween(from, to)), pageable)
                .map(room -> AdminRoomResponse.from(room, now,
                        members.countByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE))));
    }

    @Transactional
    public void close(UUID roomId) {
        lifecycle.close(rooms.findById(roomId).orElseThrow(() -> new ApiException(ErrorCode.ROOM_NOT_FOUND)));
    }
}
