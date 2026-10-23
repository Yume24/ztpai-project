package com.rendezvous.room.dto;

import com.rendezvous.room.domain.VisibilityMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateRoomRequest(
        @NotNull Long version,
        @Size(min = 1, max = 60) String name,
        Instant startsAt,
        Instant endsAt,
        VisibilityMode visibilityMode,
        Boolean membersSeeTrails) {

    public UpdateRoomRequest {
        name = name == null ? null : name.trim();
    }
}
