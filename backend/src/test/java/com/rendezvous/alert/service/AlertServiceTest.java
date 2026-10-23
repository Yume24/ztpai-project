package com.rendezvous.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rendezvous.alert.domain.Alert;
import com.rendezvous.alert.domain.AlertType;
import com.rendezvous.alert.event.AlertCreatedEvent;
import com.rendezvous.alert.repository.AlertRepository;
import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.room.service.MemberContext;
import com.rendezvous.room.service.RoomAccessService;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class AlertServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final AlertRepository alerts = mock(AlertRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoomAccessService access = mock(RoomAccessService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AlertService service = new AlertService(alerts, rooms, users, access, events,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID roomId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final User owner = new User("owner@x.pl", "hash", "Owner", UserRole.USER, NOW);
    private final User subject = new User("anna@x.pl", "hash", "Anna", UserRole.USER, NOW);
    private final Room room = new Room("Trip", "ABC234", owner, NOW, NOW.plusSeconds(3600), NOW);

    @Test
    void recordPublishesAlertCreatedWithNames() {
        UUID alertId = UUID.randomUUID();
        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(users.findById(subjectId)).thenReturn(Optional.of(subject));
        when(alerts.save(any(Alert.class))).thenAnswer(call -> {
            Alert alert = call.getArgument(0);
            ReflectionTestUtils.setField(alert, "id", alertId);
            return alert;
        });

        service.record(roomId, subjectId, AlertType.GEOFENCE_EXIT);

        ArgumentCaptor<Alert> saved = ArgumentCaptor.forClass(Alert.class);
        verify(alerts).save(saved.capture());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        verify(events).publishEvent(new AlertCreatedEvent(alertId, roomId, "Trip", subjectId, "Anna",
                AlertType.GEOFENCE_EXIT));
    }

    @Test
    void memberCannotListAlerts() {
        UUID viewerId = UUID.randomUUID();
        when(access.requireMember(roomId, viewerId)).thenReturn(
                new MemberContext(room, new RoomMember(room, subject, RoomRole.MEMBER, NOW)));

        assertThatThrownBy(() -> service.list(roomId, viewerId, null, Pageable.unpaged()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ROOM_ACTION_FORBIDDEN));
    }
}
