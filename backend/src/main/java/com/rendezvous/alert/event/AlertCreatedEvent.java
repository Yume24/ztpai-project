package com.rendezvous.alert.event;

import com.rendezvous.alert.domain.AlertType;
import java.util.UUID;

public record AlertCreatedEvent(UUID alertId, UUID roomId, String roomName, UUID subjectUserId,
                                String subjectDisplayName, AlertType type) {
}
