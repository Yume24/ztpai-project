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
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.domain.VisibilityMode;
import com.rendezvous.room.dto.CreateRoomRequest;
import com.rendezvous.room.dto.UpdateRoomRequest;
import com.rendezvous.room.event.RoomUpdatedEvent;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class RoomServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomMemberRepository members = mock(RoomMemberRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoomCodeGenerator codes = mock(RoomCodeGenerator.class);
    private final RoomAccessService access = mock(RoomAccessService.class);
    private final RoomDetailsAssembler assembler = mock(RoomDetailsAssembler.class);
    private final RoomLifecycleService lifecycle = mock(RoomLifecycleService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final RoomService service = new RoomService(rooms, members, users, codes, access, assembler,
            lifecycle, events, clock);

    private final User owner = new User("owner@x.pl", "hash", "Owner", UserRole.USER, NOW);
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void stubUser() {
        when(users.findById(userId)).thenReturn(Optional.of(owner));
        when(codes.generate()).thenReturn("ABC234");
    }

    @Test
    void createDefaultsStartToNowAndMarksStartNotified() {
        service.create(userId, new CreateRoomRequest("Trip", null, NOW.plus(Duration.ofHours(2))));

        ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);
        verify(rooms).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStartsAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getStartNotifiedAt()).isEqualTo(NOW);
        ArgumentCaptor<RoomMember> member = ArgumentCaptor.forClass(RoomMember.class);
        verify(members).save(member.capture());
        assertThat(member.getValue().getRole()).isEqualTo(RoomRole.OWNER);
    }

    @Test
    void createScheduledRoomDoesNotMarkStartNotified() {
        Instant startsAt = NOW.plus(Duration.ofDays(1));

        service.create(userId, new CreateRoomRequest("Trip", startsAt, startsAt.plus(Duration.ofHours(2))));

        ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);
        verify(rooms).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStartNotifiedAt()).isNull();
    }

    @Test
    void createRetriesCodeOnCollision() {
        when(codes.generate()).thenReturn("TAKEN2", "FRESH3");
        when(rooms.existsByCode("TAKEN2")).thenReturn(true);
        when(rooms.existsByCode("FRESH3")).thenReturn(false);

        service.create(userId, new CreateRoomRequest("Trip", null, NOW.plus(Duration.ofHours(2))));

        ArgumentCaptor<Room> saved = ArgumentCaptor.forClass(Room.class);
        verify(rooms).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCode()).isEqualTo("FRESH3");
    }

    @Test
    void updateRejectsStaleVersion() {
        Room room = activeRoom(4L);
        grantAccess(room, RoomRole.OWNER);

        assertThatThrownBy(() -> service.update(room.getId(), userId, new UpdateRoomRequest(3L, "New", null, null,
                null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ROOM_VERSION_CONFLICT));
    }

    @Test
    void memberCannotUpdateRoom() {
        Room room = activeRoom(1L);
        grantAccess(room, RoomRole.MEMBER);

        assertThatThrownBy(() -> service.update(room.getId(), userId, new UpdateRoomRequest(1L, "New", null, null,
                null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ROOM_ACTION_FORBIDDEN));
    }

    @Test
    void updatePublishesRoomUpdatedEvent() {
        Room room = activeRoom(1L);
        grantAccess(room, RoomRole.ADMIN);

        service.update(room.getId(), userId, new UpdateRoomRequest(1L, "New", null, null,
                VisibilityMode.GUARDIANS_ONLY, null));

        assertThat(room.getName()).isEqualTo("New");
        assertThat(room.getVisibilityMode()).isEqualTo(VisibilityMode.GUARDIANS_ONLY);
        verify(rooms).saveAndFlush(room);
        verify(events).publishEvent(new RoomUpdatedEvent(room.getId()));
    }

    @Test
    void deletingRoomEndedByTimeProcessesItsEndFirst() {
        Room room = new Room("Trip", "ABC234", owner, NOW.minus(Duration.ofHours(2)), NOW.minusSeconds(1),
                NOW.minus(Duration.ofHours(2)));
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        grantAccess(room, RoomRole.OWNER);

        service.delete(room.getId(), userId);

        InOrder order = inOrder(lifecycle, rooms);
        order.verify(lifecycle).processEnd(room);
        order.verify(rooms).delete(room);
        verify(lifecycle, never()).close(any());
    }

    private Room activeRoom(long version) {
        Room room = new Room("Trip", "ABC234", owner, NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(1)));
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(room, "version", version);
        return room;
    }

    private void grantAccess(Room room, RoomRole role) {
        RoomMember member = new RoomMember(room, owner, role, NOW);
        when(access.requireMember(room.getId(), userId)).thenReturn(new MemberContext(room, member));
    }
}
