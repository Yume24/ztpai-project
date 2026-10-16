package com.rendezvous.user.service;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.user.domain.PushToken;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.repository.PushTokenRepository;
import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushTokenService {

    private final PushTokenRepository pushTokens;
    private final UserRepository users;
    private final Clock clock;

    public PushTokenService(PushTokenRepository pushTokens, UserRepository users, Clock clock) {
        this.pushTokens = pushTokens;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void register(UUID userId, String token) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        pushTokens.findByToken(token).ifPresentOrElse(
                existing -> existing.reassignTo(user, clock.instant()),
                () -> pushTokens.save(new PushToken(user, token, clock.instant())));
    }

    @Transactional
    public void remove(UUID userId, String token) {
        pushTokens.deleteByUserIdAndToken(userId, token);
    }

    @Transactional(readOnly = true)
    public List<String> tokensOf(Collection<UUID> userIds) {
        return pushTokens.findByUserIdIn(userIds).stream().map(PushToken::getToken).toList();
    }

    @Transactional
    public void forget(Collection<String> tokens) {
        pushTokens.deleteByTokenIn(tokens);
    }
}
