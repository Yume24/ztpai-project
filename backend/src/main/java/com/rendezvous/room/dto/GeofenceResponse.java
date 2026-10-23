package com.rendezvous.room.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record GeofenceResponse(
        @Schema(requiredMode = REQUIRED) double lat,
        @Schema(requiredMode = REQUIRED) double lon,
        @Schema(requiredMode = REQUIRED) int radiusM) {
}
