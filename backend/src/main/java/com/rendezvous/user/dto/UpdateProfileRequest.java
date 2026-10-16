package com.rendezvous.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@NotBlank @Size(min = 2, max = 40) String displayName) {

    public UpdateProfileRequest {
        displayName = displayName == null ? null : displayName.trim();
    }
}
