package com.rendezvous.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.room.domain.GeofenceState;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.event.MemberLeftEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class RoomMembershipServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomMemberRepository members = mock(RoomMemberRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoomAccessService access = mock(RoomAccessService.class);
    private final RoomDetailsAssembler assembler = mock(RoomDetailsAssembler.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final RoomMembershipService service = new RoomMembershipService(rooms, members, users, access,
            assembler, events, clock);

    private final User ownerUser = user("owner");
    private final User joiner = user("joiner");
    private final UUID roomId = UUID.randomUUID();
    private final UUID joinerId = UUID.randomUUID();
    private Room room;

    @BeforeEach
    void setUp() {
        room = openRoom();
        when(rooms.findByCodeForUpdate("ABC234")).thenReturn(Optional.of(room));
        when(users.findById(joinerId)).thenReturn(Optional.of(joiner));
        when(members.findByRoomIdAndUserId(roomId, joinerId)).thenReturn(Optional.empty());
    }

    @Test
    void joinNormalizesCode() {
        service.join(joinerId, " abc234 ");

        verify(rooms).findByCodeForUpdate("ABC234");
        ArgumentCaptor<RoomMember> saved = ArgumentCaptor.forClass(RoomMember.class);
        verify(members).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(RoomRole.MEMBER);
    }

    @Test
    void joinEndedRoomIsRoomEnded() {
        room = room("Ended", NOW.minus(Duration.ofHours(3)), NOW.minus(Duration.ofHours(1)));
        when(rooms.findByCodeForUpdate("ABC234")).thenReturn(Optional.of(room));

        assertCode(() -> service.join(joinerId, "ABC234"), ErrorCode.ROOM_ENDED);
    }

    @Test
    void joinFullRoomIsRoomFull() {
        when(members.countByRoomIdAndStatus(roomId, MemberStatus.ACTIVE)).thenReturn(50L);

        assertCode(() -> service.join(joinerId, "ABC234"), ErrorCode.ROOM_FULL);
        verify(members, never()).save(any());
    }

    @Test
    void removedMemberCannotRejoin() {
        RoomMember removed = member(joiner, RoomRole.MEMBER);
        removed.remove(NOW);
        when(members.findByRoomIdAndUserId(roomId, joinerId)).thenReturn(Optional.of(removed));

        assertCode(() -> service.join(joinerId, "ABC234"), ErrorCode.MEMBER_REMOVED);
    }

    @Test
    void leftMemberRejoinsAsMember() {
        RoomMember left = member(joiner, RoomRole.ADMIN);
        left.leave(NOW.minusSeconds(60));
        when(members.findByRoomIdAndUserId(roomId, joinerId)).thenReturn(Optional.of(left));

        service.join(joinerId, "ABC234");

        assertThat(left.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(left.getRole()).isEqualTo(RoomRole.MEMBER);
    }

    @Test
    void joiningTwiceIsIdempotent() {
        RoomMember active = member(joiner, RoomRole.MEMBER);
        when(members.findByRoomIdAndUserId(roomId, joinerId)).thenReturn(Optional.of(active));

        service.join(joinerId, "ABC234");

        verify(members, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void ownerCannotLeave() {
        UUID ownerId = UUID.randomUUID();
        when(access.requireMember(roomId, ownerId))
                .thenReturn(new MemberContext(room, member(ownerUser, RoomRole.OWNER)));

        assertCode(() -> service.leave(roomId, ownerId), ErrorCode.ROOM_ACTION_FORBIDDEN);
    }

    @Test
    void adminCannotRemoveAdmin() {
        UUID adminId = UUID.randomUUID();
        UUID otherAdminId = UUID.randomUUID();
        when(access.requireMember(roomId, adminId))
                .thenReturn(new MemberContext(room, member(user("admin"), RoomRole.ADMIN)));
        when(members.findByRoomIdAndUserId(roomId, otherAdminId))
                .thenReturn(Optional.of(member(user("admin2"), RoomRole.ADMIN)));

        assertCode(() -> service.remove(roomId, adminId, otherAdminId), ErrorCode.ROOM_ACTION_FORBIDDEN);
    }

    @Test
    void changeRoleResetsGeofenceState() {
        UUID ownerId = UUID.randomUUID();
        RoomMember target = member(joiner, RoomRole.MEMBER);
        target.setGeofenceState(GeofenceState.OUTSIDE);
        when(access.requireMember(roomId, ownerId))
                .thenReturn(new MemberContext(room, member(ownerUser, RoomRole.OWNER)));
        when(members.findByRoomIdAndUserId(roomId, joinerId)).thenReturn(Optional.of(target));

        service.changeRole(roomId, ownerId, joinerId, RoomRole.ADMIN);

        assertThat(target.getRole()).isEqualTo(RoomRole.ADMIN);
        assertThat(target.getGeofenceState()).isEqualTo(GeofenceState.UNKNOWN);
    }

    @Test
    void onlyOwnerChangesRoles() {
        UUID adminId = UUID.randomUUID();
        when(access.requireMember(roomId, adminId))
                .thenReturn(new MemberContext(room, member(user("admin"), RoomRole.ADMIN)));

        assertCode(() -> service.changeRole(roomId, adminId, joinerId, RoomRole.ADMIN),
                ErrorCode.ROOM_ACTION_FORBIDDEN);
    }

    @Test
    void leavePublishesMemberLeftWithRoomEndedFlag() {
        RoomMember active = member(joiner, RoomRole.MEMBER);
        when(access.requireMember(roomId, joinerId)).thenReturn(new MemberContext(room, active));

        service.leave(roomId, joinerId);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new MemberLeftEvent(roomId, joinerId, false));
        assertThat(active.getStatus()).isEqualTo(MemberStatus.LEFT);
    }

    private Room openRoom() {
        return room("Trip", NOW.minus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(1)));
    }

    private Room room(String name, Instant startsAt, Instant endsAt) {
        Room created = new Room(name, "ABC234", ownerUser, startsAt, endsAt, startsAt);
        ReflectionTestUtils.setField(created, "id", roomId);
        return created;
    }

    private RoomMember member(User user, RoomRole role) {
        return new RoomMember(room, user, role, NOW.minusSeconds(600));
    }

    private User user(String name) {
        User created = new User(name + "@x.pl", "hash", name, UserRole.USER, NOW);
        ReflectionTestUtils.setField(created, "id", name.equals("joiner") ? joinerId : UUID.randomUUID());
        return created;
    }

    private void assertCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(code));
    }
}
