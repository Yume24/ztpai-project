package com.rendezvous.realtime;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

@Component
public class SessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SessionRegistry.class);
    private static final int SEND_TIME_LIMIT_MS = 5000;
    private static final int BUFFER_SIZE_LIMIT = 512 * 1024;

    private final Map<UUID, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void add(UUID userId, WebSocketSession session) {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
        sessions.compute(userId, (id, set) -> {
            Set<WebSocketSession> target = set == null ? ConcurrentHashMap.newKeySet() : set;
            target.add(decorated);
            return target;
        });
    }

    public void remove(UUID userId, WebSocketSession session) {
        sessions.computeIfPresent(userId, (id, set) -> {
            set.removeIf(s -> s.getId().equals(session.getId()));
            return set.isEmpty() ? null : set;
        });
    }

    public Collection<WebSocketSession> sessionsOf(UUID userId) {
        Set<WebSocketSession> set = sessions.get(userId);
        return set == null ? List.of() : List.copyOf(set);
    }

    public void closeAll(UUID userId) {
        for (WebSocketSession session : sessionsOf(userId)) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (IOException e) {
                log.warn("Closing WebSocket session {} failed", session.getId(), e);
            }
            remove(userId, session);
        }
    }
}
