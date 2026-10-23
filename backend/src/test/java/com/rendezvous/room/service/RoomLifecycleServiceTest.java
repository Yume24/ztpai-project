package com.rendezvous.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.event.RoomEndedEvent;
import com.rendezvous.room.event.RoomStartedEvent;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class RoomLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomMemberRepository members = mock(RoomMemberRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final RoomLifecycleService service = new RoomLifecycleService(rooms, members, events,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final User owner = user();
    private final UUID roomId = UUID.randomUUID();

    private User user() {
        User user = new User("lc-owner@x.pl", "hash", "Owner", UserRole.USER, NOW);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Room room(Instant startsAt, Instant endsAt) {
        Room room = new Room("Trip", "ABC234", owner, startsAt, endsAt, NOW.minus(Duration.ofDays(1)));
        ReflectionTestUtils.setField(room, "id", roomId);
        when(members.findByRoomIdAndStatus(roomId, MemberStatus.ACTIVE))
                .thenReturn(List.of(new RoomMember(room, owner, RoomRole.OWNER, NOW)));
        return room;
    }

    @Test
    void startDueRoomsMarksAndPublishesOnce() {
        Room room = room(NOW.minusSeconds(5), NOW.plus(Duration.ofHours(1)));
        when(rooms.findDueToStart(NOW)).thenReturn(List.of(room)).thenReturn(List.of());

        assertThat(service.startDueRooms()).isEqualTo(1);
        assertThat(service.startDueRooms()).isZero();

        assertThat(room.getStartNotifiedAt()).isEqualTo(NOW);
        verify(events).publishEvent(new RoomStartedEvent(roomId, "Trip"));
    }

    @Test
    void endDueRoomsPublishesRoomEndedWithMemberIds() {
        Room room = room(NOW.minus(Duration.ofHours(2)), NOW.minusSeconds(1));
        when(rooms.findDueToEnd(NOW)).thenReturn(List.of(room));

        assertThat(service.endDueRooms()).isEqualTo(1);

        assertThat(room.isEndProcessed()).isTrue();
        InOrder order = inOrder(rooms, events);
        order.verify(rooms).saveAndFlush(room);
        order.verify(events).publishEvent(new RoomEndedEvent(roomId, java.util.Set.of(owner.getId())));
    }

    @Test
    void endDueRoomsDoesNotReprocessClosedRoom() {
        Room room = room(NOW.minus(Duration.ofHours(2)), NOW.plus(Duration.ofHours(1)));
        when(rooms.findDueToEnd(NOW)).thenReturn(List.of(room));

        service.close(room);
        service.endDueRooms();

        ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());
        assertThat(published.getAllValues()).hasSize(1).first().isInstanceOf(RoomEndedEvent.class);
    }

    @Test
    void closingEndedRoomIsRoomEnded() {
        Room room = room(NOW.minus(Duration.ofHours(2)), NOW.minusSeconds(1));

        assertThatThrownBy(() -> service.close(room))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ROOM_ENDED));
        verify(events, never()).publishEvent(any(Object.class));
    }
}
