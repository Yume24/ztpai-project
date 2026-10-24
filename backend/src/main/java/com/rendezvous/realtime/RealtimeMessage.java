package com.rendezvous.realtime;

import java.util.UUID;

public record RealtimeMessage(RealtimeEventType type, UUID roomId, Object payload) {
}
