package com.rendezvous.auth.dto;

import com.rendezvous.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) @MaxUtf8Bytes(RegisterRequest.MAX_PASSWORD_BYTES) String password,
        @NotBlank @Size(min = 2, max = 40) String displayName) {

    // BCrypt hashes at most 72 bytes, so a longer password is rejected instead of truncated.
    public static final int MAX_PASSWORD_BYTES = 72;

    public RegisterRequest {
        email = email == null ? null : email.trim();
        displayName = displayName == null ? null : displayName.trim();
    }
}
