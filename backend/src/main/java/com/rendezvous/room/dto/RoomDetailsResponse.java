package com.rendezvous.room.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.domain.VisibilityMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record RoomDetailsResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String name,
        @Schema(requiredMode = REQUIRED) String code,
        @Schema(requiredMode = REQUIRED) RoomStatus status,
        @Schema(requiredMode = REQUIRED) Instant startsAt,
        @Schema(requiredMode = REQUIRED) Instant endsAt,
        Instant closedAt,
        @Schema(requiredMode = REQUIRED) VisibilityMode visibilityMode,
        @Schema(requiredMode = REQUIRED) boolean membersSeeTrails,
        @Schema(requiredMode = REQUIRED) Long version,
        @Schema(requiredMode = REQUIRED) RoomRole myRole,
        @Schema(requiredMode = REQUIRED) long memberCount,
        @Schema(requiredMode = REQUIRED) OwnerResponse owner,
        GeofenceResponse geofence) {
}
