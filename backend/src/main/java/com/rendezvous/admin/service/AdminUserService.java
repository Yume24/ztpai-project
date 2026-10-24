package com.rendezvous.admin.service;

import com.rendezvous.admin.dto.AdminUserResponse;
import com.rendezvous.admin.event.UserBlockedEvent;
import com.rendezvous.auth.repository.RefreshTokenRepository;
import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.common.web.PageResponse;
import com.rendezvous.room.service.RoomLifecycleService;
import com.rendezvous.room.service.RoomMembershipService;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.repository.UserRepository;
import com.rendezvous.user.repository.UserSpecifications;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final RoomMembershipService membership;
    private final RoomLifecycleService lifecycle;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public AdminUserService(UserRepository users, RefreshTokenRepository refreshTokens,
                            RoomMembershipService membership, RoomLifecycleService lifecycle,
                            ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.membership = membership;
        this.lifecycle = lifecycle;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String query, Boolean blocked, Pageable pageable) {
        return PageResponse.from(users.findAll(
                UserSpecifications.matchesQuery(query).and(UserSpecifications.isBlocked(blocked)), pageable)
                .map(AdminUserResponse::from));
    }

    @Transactional
    public void block(UUID userId) {
        User user = find(userId);
        if (user.getRole() == UserRole.ADMIN) {
            throw new ApiException(ErrorCode.ADMIN_ACTION_FORBIDDEN);
        }
        if (user.isBlocked()) {
            return;
        }
        user.block(clock.instant());
        refreshTokens.deleteByUserId(userId);
        membership.removeFromOpenRooms(userId);
        lifecycle.closeOpenRoomsOwnedBy(userId);
        events.publishEvent(new UserBlockedEvent(userId));
    }

    @Transactional
    public void unblock(UUID userId) {
        find(userId).unblock();
    }

    private User find(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
