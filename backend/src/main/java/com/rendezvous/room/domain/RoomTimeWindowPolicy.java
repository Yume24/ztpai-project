package com.rendezvous.room.domain;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import java.time.Duration;
import java.time.Instant;

public final class RoomTimeWindowPolicy {

    public static final Duration MIN_DURATION = Duration.ofMinutes(15);
    public static final Duration MAX_DURATION = Duration.ofHours(72);
    public static final Duration MAX_START_AHEAD = Duration.ofDays(30);
    public static final Duration CLOCK_TOLERANCE = Duration.ofMinutes(1);

    private RoomTimeWindowPolicy() {
    }

    public static void validateNew(Instant startsAt, Instant endsAt, Instant now) {
        if (startsAt.isBefore(now.minus(CLOCK_TOLERANCE))) {
            throw invalid("startsAt is in the past");
        }
        if (startsAt.isAfter(now.plus(MAX_START_AHEAD))) {
            throw invalid("startsAt is more than 30 days ahead");
        }
        validateDuration(startsAt, endsAt);
    }

    public static void validateChange(Room room, Instant newStartsAt, Instant newEndsAt, Instant now) {
        switch (room.statusAt(now)) {
            case ENDED -> throw new ApiException(ErrorCode.ROOM_ENDED);
            case SCHEDULED -> validateNew(newStartsAt, newEndsAt, now);
            case ACTIVE -> validateActiveChange(room, newStartsAt, newEndsAt);
        }
    }

    private static void validateActiveChange(Room room, Instant newStartsAt, Instant newEndsAt) {
        if (!newStartsAt.equals(room.getStartsAt())) {
            throw invalid("startsAt of an active room cannot change");
        }
        if (newEndsAt.isBefore(room.getEndsAt())) {
            throw invalid("endsAt of an active room can only be extended");
        }
        if (Duration.between(room.getStartsAt(), newEndsAt).compareTo(MAX_DURATION) > 0) {
            throw invalid("room duration exceeds 72 hours");
        }
    }

    private static void validateDuration(Instant startsAt, Instant endsAt) {
        Duration duration = Duration.between(startsAt, endsAt);
        if (duration.compareTo(MIN_DURATION) < 0) {
            throw invalid("room duration is shorter than 15 minutes");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw invalid("room duration exceeds 72 hours");
        }
    }

    private static ApiException invalid(String detail) {
        return new ApiException(ErrorCode.INVALID_TIME_WINDOW, detail);
    }
}
