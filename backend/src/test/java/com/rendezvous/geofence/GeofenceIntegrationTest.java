package com.rendezvous.geofence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.room.domain.GeofenceState;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.room.repository.GeofenceRepository;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

class GeofenceIntegrationTest extends IntegrationTest {

    private static final String RYNEK = "{\"lat\":50.0614,\"lon\":19.9366,\"radiusM\":300}";

    @Autowired
    private GeofenceRepository geofences;

    @Autowired
    private RoomMemberRepository members;

    @Autowired
    private TransactionTemplate tx;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private ResultActions putGeofence(String token, RoomDetailsResponse room, String body) throws Exception {
        return mvc.perform(put("/api/rooms/{id}/geofence", room.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void ownerSetsGeofenceAndRoomDetailsShowIt() throws Exception {
        String owner = api.token("geo-owner@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);

        putGeofence(owner, room, RYNEK)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.radiusM").value(300));

        mvc.perform(get("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geofence.radiusM").value(300))
                .andExpect(jsonPath("$.geofence.lat").value(50.0614))
                .andExpect(jsonPath("$.geofence.lon").value(19.9366));

        mvc.perform(delete("/api/rooms/{id}/geofence", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/rooms/{id}/geofence", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$.geofence").isEmpty());
    }

    @Test
    void radiusOutsideRangeIsRejected() throws Exception {
        String owner = api.token("geo-range-owner@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);

        putGeofence(owner, room, "{\"lat\":50.0614,\"lon\":19.9366,\"radiusM\":49}")
                .andExpect(status().isBadRequest());
        putGeofence(owner, room, "{\"lat\":50.0614,\"lon\":19.9366,\"radiusM\":5001}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void memberCannotSetGeofence() throws Exception {
        String owner = api.token("geo-perm-owner@x.pl");
        String guest = api.token("geo-perm-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest, room.code());

        putGeofence(guest, room, RYNEK)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_ACTION_FORBIDDEN"));
    }

    @Test
    void postgisContainsUsesMeters() throws Exception {
        String owner = api.token("geo-meters-owner@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        putGeofence(owner, room, RYNEK).andExpect(status().isOk());

        assertThat(geofences.contains(room.id(), 50.0632, 19.9366)).contains(true);
        assertThat(geofences.contains(room.id(), 50.0659, 19.9366)).contains(false);
        assertThat(geofences.contains(UUID.randomUUID(), 50.0632, 19.9366)).isEmpty();
    }

    @Test
    void changingGeofenceResetsMemberStates() throws Exception {
        String owner = api.token("geo-reset-owner@x.pl");
        String guest = api.token("geo-reset-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest, room.code());
        tx.executeWithoutResult(status -> members.findByRoomId(room.id())
                .forEach(member -> member.setGeofenceState(GeofenceState.OUTSIDE)));

        putGeofence(owner, room, RYNEK).andExpect(status().isOk());

        assertThat(members.findByRoomId(room.id()))
                .extracting(RoomMember::getGeofenceState)
                .containsOnly(GeofenceState.UNKNOWN);
    }
}
