package com.rendezvous.geofence.domain;

import com.rendezvous.alert.domain.AlertType;
import com.rendezvous.room.domain.GeofenceState;
import java.util.Optional;

public final class GeofenceStateMachine {

    public record Transition(GeofenceState next, Optional<AlertType> alert) {
    }

    private GeofenceStateMachine() {
    }

    public static Transition apply(GeofenceState current, boolean inside) {
        if (inside) {
            return current == GeofenceState.OUTSIDE
                    ? new Transition(GeofenceState.INSIDE, Optional.of(AlertType.GEOFENCE_RETURN))
                    : new Transition(GeofenceState.INSIDE, Optional.empty());
        }
        return current == GeofenceState.OUTSIDE
                ? new Transition(GeofenceState.OUTSIDE, Optional.empty())
                : new Transition(GeofenceState.OUTSIDE, Optional.of(AlertType.GEOFENCE_EXIT));
    }
}
