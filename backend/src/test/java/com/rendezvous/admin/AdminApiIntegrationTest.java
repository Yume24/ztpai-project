package com.rendezvous.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.support.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AdminApiIntegrationTest extends IntegrationTest {

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String randomToken() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void regularUserGets403OnAdminEndpoints() throws Exception {
        String user = api.token("plain-" + randomToken() + "@x.pl");

        mvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void usersAreSearchableAndPaginated() throws Exception {
        String token = randomToken();
        for (int i = 1; i <= 3; i++) {
            api.register("kasia-" + token + "-" + i + "@x.pl");
        }

        mvc.perform(get("/api/admin/users").param("query", token.toUpperCase()).param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(api.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void pageSizeIsCappedAtHundred() throws Exception {
        mvc.perform(get("/api/admin/users").param("size", "500")
                        .header(HttpHeaders.AUTHORIZATION, bearer(api.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void blockedUserCannotLoginAndLeavesRooms() throws Exception {
        String token = randomToken();
        String ownerEmail = "owner-" + token + "@x.pl";
        String guestEmail = "guest-" + token + "@x.pl";
        String owner = api.token(ownerEmail);
        AuthResponse guest = api.register(guestEmail);
        RoomDetailsResponse room = api.createActiveRoom(owner);
        api.join(guest.accessToken(), room.code());
        String admin = api.adminToken();

        mvc.perform(put("/api/admin/users/{id}/block", guest.user().id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + guestEmail + "\",\"password\":\"" + "password123" + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_BLOCKED"));
        mvc.perform(get("/api/rooms/{id}/members", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void roomsListShowsOnlyMetadata() throws Exception {
        String owner = api.token("meta-" + randomToken() + "@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);

        String body = mvc.perform(get("/api/admin/rooms").param("status", "ACTIVE").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(api.adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + room.id() + "')].memberCount").value(1))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("\"code\"").doesNotContain("geofence");
    }

    @Test
    void adminForceClosesRoom() throws Exception {
        String owner = api.token("close-" + randomToken() + "@x.pl");
        RoomDetailsResponse room = api.createActiveRoom(owner);
        String admin = api.adminToken();

        mvc.perform(post("/api/admin/rooms/{id}/close", room.id()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/rooms").param("status", "ENDED").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + room.id() + "')]").isNotEmpty());
    }
}
