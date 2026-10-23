package com.rendezvous.room.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.domain.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record RoomSummaryResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String name,
        @Schema(requiredMode = REQUIRED) RoomStatus status,
        @Schema(requiredMode = REQUIRED) Instant startsAt,
        @Schema(requiredMode = REQUIRED) Instant endsAt,
        @Schema(requiredMode = REQUIRED) RoomRole myRole,
        @Schema(requiredMode = REQUIRED) long memberCount) {
}
