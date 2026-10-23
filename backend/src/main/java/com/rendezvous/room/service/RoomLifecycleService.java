package com.rendezvous.room.service;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.event.RoomEndedEvent;
import com.rendezvous.room.event.RoomStartedEvent;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomLifecycleService {

    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RoomLifecycleService(RoomRepository rooms, RoomMemberRepository members,
                                ApplicationEventPublisher events, Clock clock) {
        this.rooms = rooms;
        this.members = members;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void close(Room room) {
        Instant now = clock.instant();
        if (room.statusAt(now) == RoomStatus.ENDED) {
            throw new ApiException(ErrorCode.ROOM_ENDED);
        }
        room.close(now);
        processEnd(room);
    }

    @Transactional
    public void processEnd(Room room) {
        if (room.isEndProcessed()) {
            return;
        }
        room.markEndProcessed(clock.instant());
        rooms.saveAndFlush(room);
        Set<UUID> memberUserIds = members.findByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE).stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());
        events.publishEvent(new RoomEndedEvent(room.getId(), memberUserIds));
    }

    @Transactional
    public int startDueRooms() {
        Instant now = clock.instant();
        List<Room> due = rooms.findDueToStart(now);
        for (Room room : due) {
            room.markStartNotified(now);
            events.publishEvent(new RoomStartedEvent(room.getId(), room.getName()));
        }
        return due.size();
    }

    @Transactional
    public int endDueRooms() {
        List<Room> due = rooms.findDueToEnd(clock.instant());
        due.forEach(this::processEnd);
        return due.size();
    }

    @Transactional
    public void closeOpenRoomsOwnedBy(UUID ownerId) {
        rooms.findOpenOwnedBy(ownerId, clock.instant()).forEach(this::close);
    }
}
