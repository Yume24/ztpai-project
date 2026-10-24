package com.rendezvous.location.service;

import com.rendezvous.location.repository.LocationPointRepository;
import com.rendezvous.room.event.RoomEndedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LocationRetentionListener {

    private final LocationPointRepository points;

    public LocationRetentionListener(LocationPointRepository points) {
        this.points = points;
    }

    @EventListener
    public void onRoomEnded(RoomEndedEvent event) {
        points.deleteByRoomId(event.roomId());
    }
}
