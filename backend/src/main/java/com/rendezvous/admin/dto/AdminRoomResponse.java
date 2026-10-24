package com.rendezvous.admin.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.dto.OwnerResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record AdminRoomResponse(
        @Schema(requiredMode = REQUIRED) UUID id,
        @Schema(requiredMode = REQUIRED) String name,
        @Schema(requiredMode = REQUIRED) RoomStatus status,
        @Schema(requiredMode = REQUIRED) Instant startsAt,
        @Schema(requiredMode = REQUIRED) Instant endsAt,
        Instant closedAt,
        @Schema(requiredMode = REQUIRED) OwnerResponse owner,
        @Schema(requiredMode = REQUIRED) long memberCount) {

    public static AdminRoomResponse from(Room room, Instant now, long memberCount) {
        return new AdminRoomResponse(room.getId(), room.getName(), room.statusAt(now), room.getStartsAt(),
                room.getEndsAt(), room.getClosedAt(), OwnerResponse.from(room.getOwner()), memberCount);
    }
}
