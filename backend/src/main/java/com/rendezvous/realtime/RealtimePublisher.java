package com.rendezvous.realtime;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimePublisher.class);

    private final SessionRegistry registry;
    private final JsonMapper json;

    public RealtimePublisher(SessionRegistry registry, JsonMapper json) {
        this.registry = registry;
        this.json = json;
    }

    public void send(Collection<UUID> userIds, RealtimeMessage message) {
        if (userIds.isEmpty()) {
            return;
        }
        TextMessage text = new TextMessage(json.writeValueAsString(message));
        for (UUID userId : userIds) {
            for (WebSocketSession session : registry.sessionsOf(userId)) {
                send(userId, session, text);
            }
        }
    }

    private void send(UUID userId, WebSocketSession session, TextMessage text) {
        try {
            if (session.isOpen()) {
                session.sendMessage(text);
            } else {
                registry.remove(userId, session);
            }
        } catch (IOException | RuntimeException e) {
            log.warn("Sending WebSocket message to session {} failed", session.getId(), e);
        }
    }
}
