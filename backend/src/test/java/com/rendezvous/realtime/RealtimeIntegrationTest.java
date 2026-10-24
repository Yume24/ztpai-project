package com.rendezvous.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.support.IntegrationTest;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RealtimeIntegrationTest extends IntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JsonMapper json;

    @Autowired
    private SessionRegistry registry;

    private final List<WebSocketSession> open = new ArrayList<>();
    private final List<String> seen = new ArrayList<>();

    @AfterEach
    void closeSessions() throws Exception {
        for (WebSocketSession session : open) {
            if (session.isOpen()) {
                session.close(CloseStatus.NORMAL);
            }
        }
    }

    private BlockingQueue<String> connect(AuthResponse user) throws Exception {
        BlockingQueue<String> received = handshake(user.accessToken());
        await().atMost(Duration.ofSeconds(5)).until(() -> !registry.sessionsOf(user.user().id()).isEmpty());
        return received;
    }

    private BlockingQueue<String> handshake(String token) throws Exception {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        if (token != null) {
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        WebSocketSession session = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                received.add(message.getPayload());
            }
        }, headers, URI.create("ws://localhost:" + port + "/ws")).get(5, TimeUnit.SECONDS);
        open.add(session);
        return received;
    }

    private JsonNode awaitMessage(BlockingQueue<String> queue, String type) {
        await().atMost(Duration.ofSeconds(5)).until(() -> {
            queue.drainTo(seen);
            return seen.stream().anyMatch(raw -> type.equals(json.readTree(raw).get("type").asString()));
        });
        return seen.stream().map(json::readTree)
                .filter(node -> type.equals(node.get("type").asString())).findFirst().orElseThrow();
    }

    @Test
    void handshakeWithoutTokenIsRejected() {
        assertThatThrownBy(() -> handshake(null)).hasMessageContaining("401");
    }

    @Test
    void memberReceivesLocationUpdate() throws Exception {
        String ownerToken = api.token("ws-loc-a@x.pl");
        AuthResponse member = api.register("ws-loc-b@x.pl");
        var room = api.createActiveRoom(ownerToken);
        api.join(member.accessToken(), room.code());
        BlockingQueue<String> queue = connect(member);

        api.postLocation(ownerToken, 52.0, 21.0, 10f, Instant.now());

        JsonNode message = awaitMessage(queue, "LOCATION_UPDATED");
        assertThat(message.get("roomId").asString()).isEqualTo(room.id().toString());
        assertThat(UUID.fromString(message.get("payload").get("userId").asString()))
                .isEqualTo(room.owner().id());
    }

    @Test
    void ownerReceivesAlertCreated() throws Exception {
        AuthResponse owner = api.register("ws-alert-a@x.pl");
        String memberToken = api.token("ws-alert-b@x.pl");
        var room = api.createActiveRoom(owner.accessToken());
        api.join(memberToken, room.code());
        BlockingQueue<String> queue = connect(owner);

        mvc.perform(post("/api/rooms/{id}/leave", room.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().is2xxSuccessful());

        JsonNode alert = awaitMessage(queue, "ALERT_CREATED");
        assertThat(alert.get("payload").get("type").asString()).isEqualTo("MEMBER_LEFT");
        awaitMessage(queue, "MEMBER_LEFT");
    }

    @Test
    void membersReceiveRoomEndedAfterClose() throws Exception {
        String ownerToken = api.token("ws-end-a@x.pl");
        AuthResponse member = api.register("ws-end-b@x.pl");
        var room = api.createActiveRoom(ownerToken);
        api.join(member.accessToken(), room.code());
        BlockingQueue<String> queue = connect(member);

        mvc.perform(post("/api/rooms/{id}/close", room.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().is2xxSuccessful());

        JsonNode message = awaitMessage(queue, "ROOM_ENDED");
        assertThat(message.get("roomId").asString()).isEqualTo(room.id().toString());
    }
}
