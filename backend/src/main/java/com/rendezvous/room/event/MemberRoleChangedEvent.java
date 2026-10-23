package com.rendezvous.room.event;

import java.util.UUID;

public record MemberRoleChangedEvent(UUID roomId, UUID userId) {
}
