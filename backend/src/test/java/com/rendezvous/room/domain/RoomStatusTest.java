package com.rendezvous.room.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoomStatusTest {

    private static final Instant START = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant END = Instant.parse("2026-11-14T12:00:00Z");

    private final Room room = new Room("Trip", "ABC234",
            new User("a@b.pl", "hash", "Ann", UserRole.USER, START), START, END, START);

    @Test
    void scheduledBeforeStart() {
        assertThat(room.statusAt(Instant.parse("2026-11-14T09:59:00Z"))).isEqualTo(RoomStatus.SCHEDULED);
    }

    @Test
    void activeFromStartInclusive() {
        assertThat(room.statusAt(START)).isEqualTo(RoomStatus.ACTIVE);
    }

    @Test
    void endedAtEndExclusive() {
        assertThat(room.statusAt(END)).isEqualTo(RoomStatus.ENDED);
    }

    @Test
    void closedRoomIsEndedEvenBeforeEndsAt() {
        Instant closedAt = Instant.parse("2026-11-14T10:30:00Z");
        room.close(closedAt);

        assertThat(room.statusAt(Instant.parse("2026-11-14T10:31:00Z"))).isEqualTo(RoomStatus.ENDED);
        assertThat(room.effectiveEnd()).isEqualTo(closedAt);
    }

    @Test
    void closedBeforeStartIsEnded() {
        room.close(Instant.parse("2026-11-14T09:00:00Z"));

        assertThat(room.statusAt(Instant.parse("2026-11-14T09:30:00Z"))).isEqualTo(RoomStatus.ENDED);
    }
}
