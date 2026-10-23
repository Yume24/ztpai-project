package com.rendezvous.room.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(@NotBlank String code) {
}
