package com.rendezvous.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rendezvous.admin.event.UserBlockedEvent;
import com.rendezvous.auth.repository.RefreshTokenRepository;
import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.room.service.RoomLifecycleService;
import com.rendezvous.room.service.RoomMembershipService;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AdminUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final RoomMembershipService membership = mock(RoomMembershipService.class);
    private final RoomLifecycleService lifecycle = mock(RoomLifecycleService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AdminUserService service = new AdminUserService(users, refreshTokens, membership, lifecycle,
            events, Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID userId = UUID.randomUUID();

    private User stored(UserRole role) {
        User user = new User("u@x.pl", "hash", "u", role, NOW);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void blockingRemovesSessionsMembershipsAndClosesOwnedRooms() {
        User user = stored(UserRole.USER);

        service.block(userId);

        assertThat(user.isBlocked()).isTrue();
        verify(refreshTokens).deleteByUserId(userId);
        verify(membership).removeFromOpenRooms(userId);
        verify(lifecycle).closeOpenRoomsOwnedBy(userId);
        verify(events).publishEvent(new UserBlockedEvent(userId));
    }

    @Test
    void adminCannotBeBlocked() {
        User admin = stored(UserRole.ADMIN);

        assertThatThrownBy(() -> service.block(userId))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ADMIN_ACTION_FORBIDDEN));
        assertThat(admin.isBlocked()).isFalse();
        verifyNoInteractions(refreshTokens, membership, lifecycle, events);
    }

    @Test
    void blockingTwiceIsNoOp() {
        User user = stored(UserRole.USER);
        user.block(NOW.minusSeconds(60));

        service.block(userId);

        assertThat(user.getBlockedAt()).isEqualTo(NOW.minusSeconds(60));
        verify(refreshTokens, never()).deleteByUserId(userId);
        verifyNoInteractions(membership, lifecycle, events);
    }
}
