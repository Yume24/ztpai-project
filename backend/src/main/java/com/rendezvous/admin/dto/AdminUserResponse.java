package com.rendezvous.admin.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String email,
        @Schema(requiredMode = REQUIRED) String displayName,
        @Schema(requiredMode = REQUIRED) UserRole role,
        @Schema(requiredMode = REQUIRED) boolean blocked,
        @Schema(requiredMode = REQUIRED) Instant createdAt) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
                user.isBlocked(), user.getCreatedAt());
    }
}
