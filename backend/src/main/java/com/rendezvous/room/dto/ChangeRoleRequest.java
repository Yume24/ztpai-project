package com.rendezvous.room.dto;

import com.rendezvous.room.domain.RoomRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull RoomRole role) {
}
