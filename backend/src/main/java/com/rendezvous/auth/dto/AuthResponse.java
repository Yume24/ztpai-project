package com.rendezvous.auth.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(requiredMode = REQUIRED) String accessToken,
        @Schema(requiredMode = REQUIRED) String refreshToken,
        @Schema(requiredMode = REQUIRED) UserResponse user) {
}
