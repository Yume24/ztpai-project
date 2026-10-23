package com.rendezvous.geofence.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.rendezvous.alert.domain.AlertType;
import com.rendezvous.geofence.domain.GeofenceStateMachine.Transition;
import com.rendezvous.room.domain.GeofenceState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeofenceStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "UNKNOWN,true,INSIDE,", "UNKNOWN,false,OUTSIDE,GEOFENCE_EXIT", "INSIDE,false,OUTSIDE,GEOFENCE_EXIT",
            "OUTSIDE,true,INSIDE,GEOFENCE_RETURN", "INSIDE,true,INSIDE,", "OUTSIDE,false,OUTSIDE,"})
    void transitions(GeofenceState current, boolean inside, GeofenceState next, AlertType alert) {
        Transition transition = GeofenceStateMachine.apply(current, inside);

        assertThat(transition.next()).isEqualTo(next);
        assertThat(transition.alert()).isEqualTo(java.util.Optional.ofNullable(alert));
    }
}
