package com.rendezvous.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateRoomRequest(@NotBlank @Size(max = 60) String name, Instant startsAt, @NotNull Instant endsAt) {

    public CreateRoomRequest {
        name = name == null ? null : name.trim();
    }
}
