package com.rendezvous.alert.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.alert.domain.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) AlertType type,
        @Schema(requiredMode = REQUIRED) SubjectResponse subject,
        @Schema(requiredMode = REQUIRED) Instant createdAt) {
}
