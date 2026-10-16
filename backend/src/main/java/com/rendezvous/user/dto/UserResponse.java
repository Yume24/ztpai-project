package com.rendezvous.user.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record UserResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String email,
        @Schema(requiredMode = REQUIRED) String displayName,
        @Schema(requiredMode = REQUIRED) UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
