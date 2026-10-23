package com.rendezvous.location.event;

import com.rendezvous.location.dto.PositionResponse;
import java.util.UUID;

public record LocationsRecordedEvent(UUID roomId, PositionResponse latest) {
}
