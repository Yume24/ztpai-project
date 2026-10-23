package com.rendezvous.room.event;

import java.util.UUID;

public record RoomStartedEvent(UUID roomId, String roomName) {
}
