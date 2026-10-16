package com.rendezvous.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.auth.dto.LoginRequest;
import com.rendezvous.auth.dto.RegisterRequest;
import com.rendezvous.location.dto.IngestResponse;
import com.rendezvous.location.dto.LocationBatchRequest;
import com.rendezvous.location.dto.LocationPointRequest;
import com.rendezvous.room.dto.CreateRoomRequest;
import com.rendezvous.room.dto.JoinRoomRequest;
import com.rendezvous.room.dto.RoomDetailsResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

public class TestApi {

    public static final String PASSWORD = "password123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    public AuthResponse register(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        String padded = localPart.length() < 2 ? localPart + "_" : localPart;
        String displayName = padded.substring(0, Math.min(padded.length(), 40));
        try {
            String body = mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(
                                    new RegisterRequest(email, PASSWORD, displayName))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            return json.readValue(body, AuthResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Registration of " + email + " failed", e);
        }
    }

    public String token(String email) {
        return register(email).accessToken();
    }

    public String adminToken() {
        try {
            String body = mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(
                                    new LoginRequest("admin@rendezvous.test", "admin-pass-123"))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return json.readValue(body, AuthResponse.class).accessToken();
        } catch (Exception e) {
            throw new IllegalStateException("Admin login failed", e);
        }
    }

    public RoomDetailsResponse createRoom(String token, String name, Instant startsAt, Instant endsAt) {
        try {
            String body = mvc.perform(post("/api/rooms")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new CreateRoomRequest(name, startsAt, endsAt))))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            return json.readValue(body, RoomDetailsResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Creation of room " + name + " failed", e);
        }
    }

    public RoomDetailsResponse join(String token, String code) {
        try {
            String body = mvc.perform(post("/api/rooms/join")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new JoinRoomRequest(code))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return json.readValue(body, RoomDetailsResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Joining room " + code + " failed", e);
        }
    }

    public RoomDetailsResponse createActiveRoom(String token) {
        Instant now = Instant.now();
        return createRoom(token, "Active room", now, now.plus(Duration.ofHours(2)));
    }

    public IngestResponse postLocation(String token, double lat, double lon, float accuracyM, Instant recordedAt) {
        try {
            String body = mvc.perform(post("/api/me/locations")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new LocationBatchRequest(
                                    List.of(new LocationPointRequest(lat, lon, accuracyM, recordedAt))))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            return json.readValue(body, IngestResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Posting a location failed", e);
        }
    }
}
