package com.rendezvous.geofence.service;

import com.rendezvous.alert.service.AlertService;
import com.rendezvous.geofence.domain.GeofenceStateMachine;
import com.rendezvous.geofence.domain.GeofenceStateMachine.Transition;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.repository.GeofenceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeofenceEvaluator {

    static final float MAX_ACCURACY_M = 100f;

    private final GeofenceRepository geofences;
    private final AlertService alerts;

    public GeofenceEvaluator(GeofenceRepository geofences, AlertService alerts) {
        this.geofences = geofences;
        this.alerts = alerts;
    }

    @Transactional
    public void evaluate(RoomMember member, double lat, double lon, float accuracyM) {
        if (member.getRole().isGuardian() || accuracyM > MAX_ACCURACY_M) {
            return;
        }
        UUID roomId = member.getRoom().getId();
        Optional<Boolean> inside = geofences.contains(roomId, lat, lon);
        if (inside.isEmpty()) {
            return;
        }
        Transition transition = GeofenceStateMachine.apply(member.getGeofenceState(), inside.get());
        member.setGeofenceState(transition.next());
        transition.alert().ifPresent(type -> alerts.record(roomId, member.getUser().getId(), type));
    }
}
