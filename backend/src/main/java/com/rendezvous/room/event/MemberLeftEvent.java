package com.rendezvous.room.event;

import java.util.UUID;

public record MemberLeftEvent(UUID roomId, UUID userId, boolean roomEnded) {
}
