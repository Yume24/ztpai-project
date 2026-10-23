package com.rendezvous.room.event;

import java.util.UUID;

public record MemberJoinedEvent(UUID roomId, UUID userId) {
}
