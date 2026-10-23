package com.rendezvous.location.repository;

import com.rendezvous.room.domain.RoomRole;
import java.time.Instant;
import java.util.UUID;

public interface LatestPositionRow {

    UUID getUserId();

    RoomRole getRole();

    double getLat();

    double getLon();

    float getAccuracyM();

    Instant getRecordedAt();
}
