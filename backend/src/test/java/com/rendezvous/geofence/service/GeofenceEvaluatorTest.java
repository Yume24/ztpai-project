package com.rendezvous.geofence.service;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.rendezvous.alert.domain.AlertType;
import com.rendezvous.alert.service.AlertService;
import com.rendezvous.room.domain.GeofenceState;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.repository.GeofenceRepository;
import com.rendezvous.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeofenceEvaluatorTest {

    private final GeofenceRepository geofences = mock(GeofenceRepository.class);
    private final AlertService alerts = mock(AlertService.class);
    private final GeofenceEvaluator evaluator = new GeofenceEvaluator(geofences, alerts);

    private final UUID roomId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Room room = mock(Room.class);
    private final User user = mock(User.class);

    @BeforeEach
    void setUp() {
        when(room.getId()).thenReturn(roomId);
        when(user.getId()).thenReturn(userId);
    }

    private RoomMember memberWithState(RoomRole role, GeofenceState state) {
        RoomMember member = new RoomMember(room, user, role, java.time.Instant.now());
        member.setGeofenceState(state);
        return member;
    }

    @Test
    void guardiansAreNotEvaluated() {
        RoomMember owner = memberWithState(RoomRole.OWNER, GeofenceState.INSIDE);

        evaluator.evaluate(owner, 50.0, 19.0, 10f);

        verifyNoInteractions(alerts);
        verify(geofences, never()).contains(any(), anyDouble(), anyDouble());
        assertThat(owner.getGeofenceState()).isEqualTo(GeofenceState.INSIDE);
    }

    @Test
    void lowAccuracyPointDoesNotChangeState() {
        RoomMember member = memberWithState(RoomRole.MEMBER, GeofenceState.INSIDE);

        evaluator.evaluate(member, 50.0, 19.0, 150f);

        verify(geofences, never()).contains(any(), anyDouble(), anyDouble());
        verifyNoInteractions(alerts);
        assertThat(member.getGeofenceState()).isEqualTo(GeofenceState.INSIDE);
    }

    @Test
    void roomWithoutGeofenceLeavesStateUnchanged() {
        RoomMember member = memberWithState(RoomRole.MEMBER, GeofenceState.INSIDE);
        when(geofences.contains(roomId, 50.0, 19.0)).thenReturn(Optional.empty());

        evaluator.evaluate(member, 50.0, 19.0, 10f);

        verifyNoInteractions(alerts);
        assertThat(member.getGeofenceState()).isEqualTo(GeofenceState.INSIDE);
    }

    @Test
    void exitRecordsAlert() {
        RoomMember member = memberWithState(RoomRole.MEMBER, GeofenceState.INSIDE);
        when(geofences.contains(roomId, 50.0, 19.0)).thenReturn(Optional.of(false));

        evaluator.evaluate(member, 50.0, 19.0, 10f);

        assertThat(member.getGeofenceState()).isEqualTo(GeofenceState.OUTSIDE);
        verify(alerts).record(roomId, userId, AlertType.GEOFENCE_EXIT);
    }
}
