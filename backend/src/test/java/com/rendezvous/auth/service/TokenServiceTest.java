package com.rendezvous.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rendezvous.auth.config.JwtProperties;
import com.rendezvous.auth.config.SecurityConfig;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

    private TokenService tokens;
    private JwtDecoder decoder;
    private JwtProperties properties;
    private final SecurityConfig securityConfig = new SecurityConfig();

    @BeforeEach
    void setUp() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        properties = new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(30));
        decoder = securityConfig.jwtDecoder(properties, clock);
        tokens = new TokenService(new NimbusJwtEncoder(new ImmutableSecret<>(key)), properties, clock);
    }

    @Test
    void accessTokenCarriesSubjectRoleAndFifteenMinuteExpiry() {
        Jwt jwt = decoder.decode(tokens.issueAccessToken(userWithId(UserRole.ADMIN)));

        assertThat(jwt.getSubject()).isEqualTo(ID.toString());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void expiredAccessTokenIsRejectedWithoutClockSkew() {
        String token = tokens.issueAccessToken(userWithId(UserRole.USER));
        Clock afterExpiry = Clock.fixed(NOW.plus(Duration.ofMinutes(15)).plusSeconds(1), ZoneOffset.UTC);

        assertThatThrownBy(() -> securityConfig.jwtDecoder(properties, afterExpiry).decode(token))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void refreshTokenIsRandomAndStoredOnlyAsHash() {
        var a = tokens.issueRefreshToken();
        var b = tokens.issueRefreshToken();

        assertThat(a.raw()).isNotEqualTo(b.raw());
        assertThat(a.hash()).isEqualTo(tokens.hash(a.raw())).isNotEqualTo(a.raw()).hasSize(64);
    }

    private static User userWithId(UserRole role) {
        User user = new User("a@b.pl", "hash", "Ala", role, NOW);
        ReflectionTestUtils.setField(user, "id", ID);
        return user;
    }
}
