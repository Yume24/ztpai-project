package com.rendezvous.room.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record OwnerResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String displayName) {

    public static OwnerResponse from(User owner) {
        return new OwnerResponse(owner.getId(), owner.getDisplayName());
    }
}
