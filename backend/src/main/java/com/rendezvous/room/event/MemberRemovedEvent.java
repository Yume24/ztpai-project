package com.rendezvous.room.event;

import java.util.UUID;

public record MemberRemovedEvent(UUID roomId, String roomName, UUID userId) {
}
