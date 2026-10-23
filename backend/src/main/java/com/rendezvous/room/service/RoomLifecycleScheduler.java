package com.rendezvous.room.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true")
public class RoomLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoomLifecycleScheduler.class);

    private final RoomLifecycleService lifecycle;

    public RoomLifecycleScheduler(RoomLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Scheduled(fixedDelayString = "PT60S")
    public void run() {
        int started = lifecycle.startDueRooms();
        int ended = lifecycle.endDueRooms();
        log.debug("Room lifecycle tick: {} started, {} ended", started, ended);
    }
}
