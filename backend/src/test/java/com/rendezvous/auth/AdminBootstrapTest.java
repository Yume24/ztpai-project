package com.rendezvous.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rendezvous.user.repository.UserRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminBootstrapTest {

    private final UserRepository users = mock(UserRepository.class);

    @Test
    void passwordOverBcryptByteLimitStopsStartupWithoutRevealingIt() {
        String password = "ż".repeat(37);
        AdminBootstrap bootstrap = new AdminBootstrap(new AdminProperties("admin@x.pl", password), users,
                new BCryptPasswordEncoder(4), Clock.systemUTC());

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.admin.password")
                .hasMessageContaining("72 bytes")
                .hasMessageNotContaining(password);
        verifyNoInteractions(users);
    }
}
