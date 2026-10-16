package com.rendezvous.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rendezvous.auth.config.JwtProperties;
import com.rendezvous.auth.domain.RefreshToken;
import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.auth.dto.LoginRequest;
import com.rendezvous.auth.dto.RegisterRequest;
import com.rendezvous.auth.repository.RefreshTokenRepository;
import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.repository.UserRepository;
import com.rendezvous.user.service.PushTokenService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final TokenService tokens = mock(TokenService.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtProperties jwtProperties = new JwtProperties(
            "test-secret-test-secret-test-secret-0123456789", Duration.ofMinutes(15), Duration.ofDays(30));
    private final PushTokenService pushTokens = mock(PushTokenService.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AuthService service = new AuthService(users, refreshTokens, tokens, encoder, jwtProperties,
            pushTokens, clock);

    @BeforeEach
    void stubTokens() {
        when(tokens.issueAccessToken(any())).thenReturn("access");
        when(tokens.issueRefreshToken()).thenReturn(new TokenService.IssuedRefreshToken("raw-new", "hash-new"));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerNormalizesEmailAndRejectsDuplicate() {
        AuthResponse response = service.register(new RegisterRequest(" Ala@X.pl ", "password123", "Ala"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ala@x.pl");
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(response.user().email()).isEqualTo("ala@x.pl");

        when(users.existsByEmail("ala@x.pl")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("ALA@x.pl", "password123", "Ala")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.EMAIL_TAKEN));
    }

    @Test
    void loginWithWrongPasswordIsInvalidCredentials() {
        when(users.findByEmail("ala@x.pl")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> service.login(new LoginRequest("ala@x.pl", "wrong-password")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void blockedUserCannotLogin() {
        User user = user();
        user.block(NOW);
        when(users.findByEmail("ala@x.pl")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("ala@x.pl", "password123")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.USER_BLOCKED));
    }

    @Test
    void refreshRotatesToken() {
        when(tokens.hash("raw-old")).thenReturn("hash-old");
        when(refreshTokens.findByTokenHash("hash-old")).thenReturn(Optional.of(
                new RefreshToken(user(), "hash-old", NOW.plus(Duration.ofDays(1)), NOW.minus(Duration.ofDays(1)))));

        when(refreshTokens.deleteByTokenHash("hash-old")).thenReturn(1);

        AuthResponse response = service.refresh("raw-old");

        verify(refreshTokens).deleteByTokenHash("hash-old");
        assertThat(response.refreshToken()).isEqualTo("raw-new");
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo("hash-new");
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        when(tokens.hash("raw-old")).thenReturn("hash-old");
        when(refreshTokens.findByTokenHash("hash-old")).thenReturn(Optional.of(
                new RefreshToken(user(), "hash-old", NOW.minusSeconds(1), NOW.minus(Duration.ofDays(30)))));

        assertThatThrownBy(() -> service.refresh("raw-old"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        verify(refreshTokens).deleteByTokenHash("hash-old");
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void refreshTokenAlreadyConsumedByConcurrentRequestIsRejected() {
        when(tokens.hash("raw-old")).thenReturn("hash-old");
        when(refreshTokens.findByTokenHash("hash-old")).thenReturn(Optional.of(
                new RefreshToken(user(), "hash-old", NOW.plus(Duration.ofDays(1)), NOW.minus(Duration.ofDays(1)))));
        when(refreshTokens.deleteByTokenHash("hash-old")).thenReturn(0);

        assertThatThrownBy(() -> service.refresh("raw-old"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void refreshForBlockedUserIsRejected() {
        User user = user();
        user.block(NOW);
        when(tokens.hash("raw-old")).thenReturn("hash-old");
        when(refreshTokens.findByTokenHash("hash-old")).thenReturn(Optional.of(
                new RefreshToken(user, "hash-old", NOW.plus(Duration.ofDays(1)), NOW.minus(Duration.ofDays(1)))));

        assertThatThrownBy(() -> service.refresh("raw-old"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.USER_BLOCKED));
    }

    private User user() {
        return new User("ala@x.pl", encoder.encode("password123"), "Ala", UserRole.USER, NOW);
    }
}
