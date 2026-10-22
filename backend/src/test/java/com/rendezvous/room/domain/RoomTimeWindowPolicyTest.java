package com.rendezvous.room.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.user.domain.User;
import com.rendezvous.user.domain.UserRole;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoomTimeWindowPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_TIME_WINDOW));
    }

    private static Room room(Instant startsAt, Instant endsAt) {
        return new Room("Trip", "ABC234", new User("a@b.pl", "hash", "Ann", UserRole.USER, NOW),
                startsAt, endsAt, NOW);
    }

    @Test
    void acceptsFifteenMinutesAndSeventyTwoHours() {
        assertThatCode(() -> RoomTimeWindowPolicy.validateNew(NOW, NOW.plus(Duration.ofMinutes(15)), NOW))
                .doesNotThrowAnyException();
        assertThatCode(() -> RoomTimeWindowPolicy.validateNew(NOW, NOW.plus(Duration.ofHours(72)), NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsShorterThanFifteenMinutes() {
        assertInvalid(() -> RoomTimeWindowPolicy.validateNew(NOW, NOW.plus(Duration.ofMinutes(14)), NOW));
    }

    @Test
    void rejectsLongerThanSeventyTwoHours() {
        assertInvalid(() -> RoomTimeWindowPolicy.validateNew(
                NOW, NOW.plus(Duration.ofHours(72)).plusSeconds(1), NOW));
    }

    @Test
    void rejectsStartInThePastBeyondTolerance() {
        assertInvalid(() -> RoomTimeWindowPolicy.validateNew(
                NOW.minus(Duration.ofMinutes(2)), NOW.plus(Duration.ofHours(1)), NOW));
        assertThatCode(() -> RoomTimeWindowPolicy.validateNew(
                NOW.minusSeconds(30), NOW.plus(Duration.ofHours(1)), NOW)).doesNotThrowAnyException();
    }

    @Test
    void rejectsStartMoreThanThirtyDaysAhead() {
        Instant limit = NOW.plus(Duration.ofDays(30));
        assertThatCode(() -> RoomTimeWindowPolicy.validateNew(limit, limit.plus(Duration.ofHours(1)), NOW))
                .doesNotThrowAnyException();
        Instant tooLate = limit.plusSeconds(1);
        assertInvalid(() -> RoomTimeWindowPolicy.validateNew(tooLate, tooLate.plus(Duration.ofHours(1)), NOW));
    }

    @Test
    void scheduledRoomAppliesNewWindowRules() {
        Room room = room(NOW.plus(Duration.ofHours(1)), NOW.plus(Duration.ofHours(2)));
        Instant start = NOW.plus(Duration.ofHours(3));

        assertThatCode(() -> RoomTimeWindowPolicy.validateChange(room, start, start.plus(Duration.ofHours(1)), NOW))
                .doesNotThrowAnyException();
        assertInvalid(() -> RoomTimeWindowPolicy.validateChange(
                room, start, start.plus(Duration.ofMinutes(5)), NOW));
    }

    @Test
    void activeRoomCanOnlyBeExtended() {
        Instant start = NOW.minus(Duration.ofHours(1));
        Instant end = NOW.plus(Duration.ofHours(1));
        Room room = room(start, end);

        assertThatCode(() -> RoomTimeWindowPolicy.validateChange(room, start, end.plusSeconds(1), NOW))
                .doesNotThrowAnyException();
        assertThatCode(() -> RoomTimeWindowPolicy.validateChange(room, start, end, NOW))
                .doesNotThrowAnyException();
        assertInvalid(() -> RoomTimeWindowPolicy.validateChange(room, start, end.minusSeconds(1), NOW));
        assertInvalid(() -> RoomTimeWindowPolicy.validateChange(
                room, start.plusSeconds(1), end.plusSeconds(1), NOW));
    }

    @Test
    void activeRoomExtensionKeepsSeventyTwoHourLimitFromStart() {
        Instant start = NOW.minus(Duration.ofHours(1));
        Room room = room(start, NOW.plus(Duration.ofHours(1)));

        assertThatCode(() -> RoomTimeWindowPolicy.validateChange(
                room, start, start.plus(Duration.ofHours(72)), NOW)).doesNotThrowAnyException();
        assertInvalid(() -> RoomTimeWindowPolicy.validateChange(
                room, start, start.plus(Duration.ofHours(72)).plusSeconds(1), NOW));
    }

    @Test
    void endedRoomCannotChange() {
        Room room = room(NOW.minus(Duration.ofHours(2)), NOW.minus(Duration.ofHours(1)));

        assertThatThrownBy(() -> RoomTimeWindowPolicy.validateChange(
                room, room.getStartsAt(), NOW.plus(Duration.ofHours(1)), NOW))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ROOM_ENDED));
    }
}
