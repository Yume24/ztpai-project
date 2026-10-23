package com.rendezvous.room.event;

import java.util.Set;
import java.util.UUID;

public record RoomEndedEvent(UUID roomId, Set<UUID> memberUserIds) {
}
