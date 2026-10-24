package com.rendezvous.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class LocationFlowIntegrationTest extends IntegrationTest {

    private static final double RYNEK_LAT = 50.0614;
    private static final double RYNEK_LON = 19.9366;
    private static final double OUTSIDE_LAT = 50.0659;

    @Autowired
    private JdbcTemplate jdbc;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private void patchRoom(String token, RoomDetailsResponse room, String fields) throws Exception {
        mvc.perform(patch("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + room.version() + "," + fields + "}"))
                .andExpect(status().isOk());
    }

    private void setGeofence(String token, RoomDetailsResponse room) throws Exception {
        mvc.perform(put("/api/rooms/{id}/geofence", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":" + RYNEK_LAT + ",\"lon\":" + RYNEK_LON + ",\"radiusM\":300}"))
                .andExpect(status().isOk());
    }

    private void walkOutAndBack(String guest) {
        Instant now = Instant.now();
        api.postLocation(guest, RYNEK_LAT, RYNEK_LON, 10f, now.plusSeconds(1));
        api.postLocation(guest, OUTSIDE_LAT, RYNEK_LON, 10f, now.plusSeconds(2));
        api.postLocation(guest, RYNEK_LAT, RYNEK_LON, 10f, now.plusSeconds(3));
    }

    private int pointCount(RoomDetailsResponse room) {
        return jdbc.queryForObject("""
                select count(*) from location_points p join room_members m on m.id = p.member_id
                where m.room_id = ?""", Integer.class, room.id());
    }

    @Test
    void latestPositionsRespectGuardiansOnlyMode() throws Exception {
        String owner = api.token("loc-latest-owner@x.pl");
        String guest1 = api.token("loc-latest-g1@x.pl");
        String guest2 = api.token("loc-latest-g2@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest1, room.code());
        api.join(guest2, room.code());
        patchRoom(owner, room, "\"visibilityMode\":\"GUARDIANS_ONLY\"");
        Instant at = Instant.now().plusSeconds(1);
        for (String token : new String[] {owner, guest1, guest2}) {
            api.postLocation(token, RYNEK_LAT, RYNEK_LON, 10f, at);
        }

        mvc.perform(get("/api/rooms/{id}/locations/latest", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/rooms/{id}/locations/latest", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(guest1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void trailRequiresPermissionForMembers() throws Exception {
        String owner = api.token("loc-trail-owner@x.pl");
        AuthResponse guest1 = api.register("loc-trail-g1@x.pl");
        String guest2 = api.token("loc-trail-g2@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest1.accessToken(), room.code());
        api.join(guest2, room.code());
        Instant now = Instant.now();
        api.postLocation(guest1.accessToken(), 50.0610, RYNEK_LON, 10f, now.plusSeconds(2));
        api.postLocation(guest1.accessToken(), 50.0611, RYNEK_LON, 10f, now.plusSeconds(1));

        mvc.perform(get("/api/rooms/{id}/members/{uid}/trail", room.id(), guest1.user().id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(guest2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_ACTION_FORBIDDEN"));

        patchRoom(owner, room, "\"membersSeeTrails\":true");

        mvc.perform(get("/api/rooms/{id}/members/{uid}/trail", room.id(), guest1.user().id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(guest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lat").value(50.0611))
                .andExpect(jsonPath("$[1].lat").value(50.0610));
    }

    @Test
    void leavingGeofenceCreatesExitAndReturnAlerts() throws Exception {
        String owner = api.token("loc-geo-owner@x.pl");
        String guest = api.token("loc-geo-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest, room.code());
        setGeofence(owner, room);

        walkOutAndBack(guest);

        mvc.perform(get("/api/rooms/{id}/alerts", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].type").value("GEOFENCE_RETURN"))
                .andExpect(jsonPath("$.content[1].type").value("GEOFENCE_EXIT"));
    }

    @Test
    void closingRoomDeletesTrailsButKeepsAlerts() throws Exception {
        String owner = api.token("loc-close-owner@x.pl");
        String guest = api.token("loc-close-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest, room.code());
        setGeofence(owner, room);
        walkOutAndBack(guest);
        assertThat(pointCount(room)).isEqualTo(3);

        mvc.perform(post("/api/rooms/{id}/close", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        assertThat(pointCount(room)).isZero();
        mvc.perform(get("/api/rooms/{id}/alerts", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void invalidCoordinatesAreRejected() throws Exception {
        String token = api.token("loc-invalid@x.pl");

        mvc.perform(post("/api/me/locations").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":[{\"lat\":91,\"lon\":19.9,\"accuracyM\":5,\"recordedAt\":\""
                                + Instant.now() + "\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void responseReportsActiveRoomCount() {
        String owner = api.token("loc-count-owner@x.pl");
        api.createActiveRoom(owner);
        Instant now = Instant.now();
        api.createRoom(owner, "Later", now.plus(Duration.ofHours(3)), now.plus(Duration.ofHours(5)));

        assertThat(api.postLocation(owner, RYNEK_LAT, RYNEK_LON, 10f, now.plusSeconds(1)).activeRoomCount())
                .isEqualTo(1);
    }
}
