package com.rendezvous.room.domain;

import java.util.UUID;

public record MemberRef(UUID userId, RoomRole role) {
}
