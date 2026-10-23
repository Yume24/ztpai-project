package com.rendezvous.location.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record IngestResponse(@Schema(requiredMode = REQUIRED) int activeRoomCount) {
}
