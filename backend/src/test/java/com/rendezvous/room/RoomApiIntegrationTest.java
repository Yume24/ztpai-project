package com.rendezvous.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

class RoomApiIntegrationTest extends IntegrationTest {

    @Autowired
    private JsonMapper json;

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void createdRoomIsReturnedWithOwnerRoleAndCode() throws Exception {
        String token = api.token("room-create@x.pl");
        Instant endsAt = Instant.now().plus(Duration.ofHours(2));

        mvc.perform(post("/api/rooms").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  Trip  \",\"endsAt\":\"" + endsAt + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Trip"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.owner.displayName").value("room-create"))
                .andExpect(jsonPath("$.geofence").doesNotExist())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("^[A-HJ-KM-NP-Z2-9]{6}$")));
    }

    @Test
    void createRejectsInvalidWindow() throws Exception {
        String token = api.token("room-window@x.pl");
        Instant startsAt = Instant.now();

        mvc.perform(post("/api/rooms").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Trip\",\"startsAt\":\"" + startsAt + "\",\"endsAt\":\""
                                + startsAt.plus(Duration.ofMinutes(10)) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_WINDOW"));
    }

    @Test
    void outsiderGets404ForForeignRoom() throws Exception {
        RoomDetailsResponse room = api.createActiveRoom(api.token("room-owner404@x.pl"));
        String outsider = api.token("room-outsider404@x.pl");

        mvc.perform(get("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void patchChangesSettingsAndBumpsVersion() throws Exception {
        String token = api.token("room-patch@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(token);
        String body = "{\"version\":" + room.version() + ",\"visibilityMode\":\"GUARDIANS_ONLY\"}";

        mvc.perform(patch("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibilityMode").value("GUARDIANS_ONLY"))
                .andExpect(jsonPath("$.version").value(room.version() + 1));

        mvc.perform(patch("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_VERSION_CONFLICT"));
    }

    @Test
    void activeRoomCanBeExtendedByEchoingItsServerAssignedStart() throws Exception {
        String token = api.token("room-echo@x.pl");
        Instant endsAt = Instant.now().plus(Duration.ofHours(2));
        String created = mvc.perform(post("/api/rooms").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Echo\",\"endsAt\":\"" + endsAt + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        RoomDetailsResponse room = json.readValue(created, RoomDetailsResponse.class);
        Instant echoedStart = room.startsAt().truncatedTo(ChronoUnit.MILLIS);

        mvc.perform(patch("/api/rooms/{id}", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + room.version() + ",\"startsAt\":\"" + echoedStart
                                + "\",\"endsAt\":\"" + endsAt.plus(Duration.ofHours(1)) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void myRoomsFiltersByStatusAndPaginates() throws Exception {
        String token = api.token("room-mine@x.pl");
        Instant tomorrow = Instant.now().plus(Duration.ofDays(1));
        api.createActiveRoom(token);
        api.createRoom(token, "Later 1", tomorrow, tomorrow.plus(Duration.ofHours(2)));
        api.createRoom(token, "Later 2", tomorrow.plus(Duration.ofHours(1)), tomorrow.plus(Duration.ofHours(3)));

        String body = mvc.perform(get("/api/me/rooms?status=SCHEDULED&size=1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].status").value(RoomStatus.SCHEDULED.name()))
                .andExpect(jsonPath("$.content[0].myRole").value(RoomRole.OWNER.name()))
                .andExpect(jsonPath("$.content[0].memberCount").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("Later 2");

        mvc.perform(get("/api/me/rooms").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }
}
