package com.rendezvous.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rendezvous.user.domain.PushToken;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.repository.PushTokenRepository;
import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PushTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final String TOKEN = "ExponentPushToken[abc123]";

    private final PushTokenRepository pushTokens = mock(PushTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PushTokenService service = new PushTokenService(pushTokens, users, clock);

    private final UUID ownerId = UUID.randomUUID();
    private final UUID otherId = UUID.randomUUID();
    private final User owner = new User("a@x.pl", "hash", "Ala", UserRole.USER, NOW);
    private final User other = new User("b@x.pl", "hash", "Ola", UserRole.USER, NOW);

    @Test
    void registeringKnownTokenForAnotherUserReassignsIt() {
        PushToken existing = new PushToken(owner, TOKEN, NOW.minusSeconds(60));
        when(users.findById(otherId)).thenReturn(Optional.of(other));
        when(pushTokens.findByToken(TOKEN)).thenReturn(Optional.of(existing));

        service.register(otherId, TOKEN);

        assertThat(existing.getUser()).isSameAs(other);
        assertThat(existing.getUpdatedAt()).isEqualTo(NOW);
        verify(pushTokens, never()).save(any());
    }

    @Test
    void registeringSameTokenTwiceDoesNotDuplicate() {
        PushToken existing = new PushToken(owner, TOKEN, NOW.minusSeconds(60));
        when(users.findById(ownerId)).thenReturn(Optional.of(owner));
        when(pushTokens.findByToken(TOKEN)).thenReturn(Optional.empty(), Optional.of(existing));

        service.register(ownerId, TOKEN);
        service.register(ownerId, TOKEN);

        verify(pushTokens, org.mockito.Mockito.times(1)).save(any(PushToken.class));
        assertThat(existing.getUpdatedAt()).isEqualTo(NOW);
    }
}
