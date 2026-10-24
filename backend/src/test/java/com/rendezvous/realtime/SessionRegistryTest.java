package com.rendezvous.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class SessionRegistryTest {

    private final SessionRegistry registry = new SessionRegistry();
    private final UUID userId = UUID.randomUUID();

    private static WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void addRemoveAddLeavesOnlyTheNewSession() {
        WebSocketSession first = session("s1");
        WebSocketSession second = session("s2");

        registry.add(userId, first);
        registry.remove(userId, first);
        registry.add(userId, second);

        assertThat(registry.sessionsOf(userId)).extracting(WebSocketSession::getId).containsExactly("s2");
    }

    @Test
    void removingLastSessionDropsTheUser() {
        WebSocketSession only = session("s1");
        registry.add(userId, only);

        registry.remove(userId, only);

        assertThat(registry.sessionsOf(userId)).isEmpty();
    }

    @Test
    void closeAllClosesEverySessionOfTheUser() throws Exception {
        WebSocketSession first = session("s1");
        WebSocketSession second = session("s2");
        registry.add(userId, first);
        registry.add(userId, second);

        registry.closeAll(userId);

        verify(first).close(CloseStatus.POLICY_VIOLATION);
        verify(second).close(CloseStatus.POLICY_VIOLATION);
        assertThat(registry.sessionsOf(userId)).isEmpty();
    }
}
