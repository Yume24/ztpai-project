package com.rendezvous.auth.service;

import com.rendezvous.auth.config.JwtProperties;
import com.rendezvous.auth.domain.RefreshToken;
import com.rendezvous.auth.dto.AuthResponse;
import com.rendezvous.auth.dto.LoginRequest;
import com.rendezvous.auth.dto.LogoutRequest;
import com.rendezvous.auth.dto.RegisterRequest;
import com.rendezvous.auth.repository.RefreshTokenRepository;
import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.dto.UserResponse;
import com.rendezvous.user.repository.UserRepository;
import com.rendezvous.user.service.PushTokenService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final PushTokenService pushTokens;
    private final Clock clock;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, TokenService tokens,
            PasswordEncoder passwordEncoder, JwtProperties jwtProperties, PushTokenService pushTokens, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
        this.pushTokens = pushTokens;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN);
        }
        User user = users.save(new User(email, passwordEncoder.encode(request.password()),
                request.displayName(), UserRole.USER, clock.instant()));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(User.normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        if (user.isBlocked()) {
            throw new ApiException(ErrorCode.USER_BLOCKED);
        }
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = tokens.hash(rawRefreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (stored.isExpired(clock.instant())) {
            refreshTokens.deleteByTokenHash(hash);
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        User user = stored.getUser();
        if (user.isBlocked()) {
            throw new ApiException(ErrorCode.USER_BLOCKED);
        }
        if (refreshTokens.deleteByTokenHash(hash) == 0) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokens.deleteByTokenHash(tokens.hash(request.refreshToken()));
        if (request.pushToken() != null && !request.pushToken().isBlank()) {
            pushTokens.forget(List.of(request.pushToken()));
        }
    }

    private AuthResponse issueTokens(User user) {
        Instant now = clock.instant();
        TokenService.IssuedRefreshToken refresh = tokens.issueRefreshToken();
        refreshTokens.save(new RefreshToken(user, refresh.hash(), now.plus(jwtProperties.refreshTokenTtl()), now));
        return new AuthResponse(tokens.issueAccessToken(user), refresh.raw(), UserResponse.from(user));
    }
}
