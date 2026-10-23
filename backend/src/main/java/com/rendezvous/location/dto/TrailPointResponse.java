package com.rendezvous.location.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record TrailPointResponse(
        @Schema(requiredMode = REQUIRED) double lat,
        @Schema(requiredMode = REQUIRED) double lon,
        @Schema(requiredMode = REQUIRED) Instant recordedAt) {
}
