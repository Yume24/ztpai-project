package com.rendezvous.auth;

import com.rendezvous.auth.dto.RegisterRequest;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import com.rendezvous.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final String ADMIN_DISPLAY_NAME = "Admin";

    private final AdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminBootstrap(AdminProperties properties, UserRepository users, PasswordEncoder passwordEncoder,
            Clock clock) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            return;
        }
        if (properties.password().getBytes(StandardCharsets.UTF_8).length > RegisterRequest.MAX_PASSWORD_BYTES) {
            throw new IllegalStateException("app.admin.password must not exceed "
                    + RegisterRequest.MAX_PASSWORD_BYTES + " bytes in UTF-8 (BCrypt limit).");
        }
        String email = User.normalizeEmail(properties.email());
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(new User(email, passwordEncoder.encode(properties.password()), ADMIN_DISPLAY_NAME,
                UserRole.ADMIN, clock.instant()));
    }
}
