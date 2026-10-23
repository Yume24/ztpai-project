package com.rendezvous.alert;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class AlertIntegrationTest extends IntegrationTest {

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void memberLeavingCreatesAlertVisibleToOwner() throws Exception {
        String owner = api.token("alert-owner@x.pl");
        String guest = api.token("alert-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest, room.code());

        mvc.perform(post("/api/rooms/{id}/leave", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(guest)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/rooms/{id}/alerts", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("MEMBER_LEFT"))
                .andExpect(jsonPath("$.content[0].subject.displayName").value("alert-guest"))
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());
    }

    @Test
    void alertsCanBeFilteredByType() throws Exception {
        String owner = api.token("alert-filter-owner@x.pl");
        String guest = api.token("alert-filter-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest, room.code());
        mvc.perform(post("/api/rooms/{id}/leave", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(guest)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/rooms/{id}/alerts", room.id()).param("type", "GEOFENCE_EXIT")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void leavingEndedRoomCreatesNoAlert() throws Exception {
        String owner = api.token("alert-ended-owner@x.pl");
        String guest = api.token("alert-ended-guest@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest, room.code());
        mvc.perform(post("/api/rooms/{id}/close", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/rooms/{id}/leave", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(guest)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/rooms/{id}/alerts", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }
}
