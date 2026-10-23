package com.rendezvous.location.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rendezvous.geofence.service.GeofenceEvaluator;
import com.rendezvous.location.domain.LocationPoint;
import com.rendezvous.location.dto.LocationPointRequest;
import com.rendezvous.location.event.LocationsRecordedEvent;
import com.rendezvous.location.repository.LocationPointRepository;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.RoomRole;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import com.rendezvous.user.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class LocationIngestServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:30Z");

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomMemberRepository members = mock(RoomMemberRepository.class);
    private final LocationPointRepository points = mock(LocationPointRepository.class);
    private final GeofenceEvaluator evaluator = mock(GeofenceEvaluator.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final LocationIngestService service = new LocationIngestService(rooms, members, points, evaluator,
            events, Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID userId = UUID.randomUUID();
    private final User user = mock(User.class);

    private RoomMember membership(Instant startsAt) {
        return membership(startsAt, startsAt.minus(Duration.ofHours(1)));
    }

    private RoomMember membership(Instant startsAt, Instant joinedAt) {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(UUID.randomUUID());
        when(room.getStartsAt()).thenReturn(startsAt);
        when(room.effectiveEnd()).thenReturn(NOW.plus(Duration.ofHours(1)));
        when(user.getId()).thenReturn(userId);
        return new RoomMember(room, user, RoomRole.MEMBER, joinedAt);
    }

    private void givenActive(RoomMember... memberships) {
        List<Room> active = Stream.of(memberships).map(RoomMember::getRoom).toList();
        List<UUID> activeIds = active.stream().map(Room::getId).toList();
        when(rooms.findActiveByMemberForShare(userId, NOW)).thenReturn(active);
        when(members.findByRoomIdInAndUserIdAndStatus(activeIds, userId, MemberStatus.ACTIVE))
                .thenReturn(List.of(memberships));
    }

    private static LocationPointRequest point(double lat, Instant recordedAt) {
        return new LocationPointRequest(lat, 19.9, 10f, recordedAt);
    }

    @SuppressWarnings("unchecked")
    private int savedCount() {
        ArgumentCaptor<List<LocationPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(points, times(1)).saveAll(captor.capture());
        return captor.getValue().size();
    }

    @Test
    void stalePointsAreDroppedSilently() {
        givenActive(membership(NOW.minusSeconds(3600)));

        int result = service.ingest(userId, List.of(
                point(50.0, NOW.minus(Duration.ofMinutes(16))),
                point(50.1, NOW.plus(Duration.ofMinutes(2)))));

        assertThat(result).isEqualTo(1);
        verify(points, never()).saveAll(any());
        verify(evaluator, never()).evaluate(any(), anyDouble(), anyDouble(), anyFloat());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void returnsZeroWhenUserHasNoActiveRooms() {
        when(rooms.findActiveByMemberForShare(userId, NOW)).thenReturn(List.of());

        assertThat(service.ingest(userId, List.of(point(50.0, NOW)))).isZero();
        verifyNoInteractions(members, points);
    }

    @Test
    void newestPointByRecordedAtDrivesGeofenceEvenWhenBatchUnsorted() {
        RoomMember member = membership(NOW.minusSeconds(3600));
        givenActive(member);
        Instant a = Instant.parse("2026-11-14T12:00:20Z");
        Instant b = Instant.parse("2026-11-14T12:00:10Z");

        service.ingest(userId, List.of(point(50.2, a), point(50.1, b)));

        verify(evaluator, times(1)).evaluate(eq(member), eq(50.2), anyDouble(), anyFloat());
        assertThat(savedCount()).isEqualTo(2);
    }

    @Test
    void ingestFansOutToEveryActiveRoomAndSkipsPointsBeforeRoomStart() {
        RoomMember x = membership(Instant.parse("2026-11-14T11:00:00Z"));
        RoomMember y = membership(Instant.parse("2026-11-14T12:00:15Z"));
        givenActive(x, y);

        int result = service.ingest(userId, List.of(
                point(50.1, Instant.parse("2026-11-14T12:00:10Z")),
                point(50.2, Instant.parse("2026-11-14T12:00:20Z"))));

        assertThat(result).isEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LocationPoint>> captor = ArgumentCaptor.forClass(List.class);
        verify(points, times(2)).saveAll(captor.capture());
        assertThat(captor.getAllValues()).extracting(List::size).containsExactly(2, 1);
        ArgumentCaptor<LocationsRecordedEvent> published = ArgumentCaptor.forClass(LocationsRecordedEvent.class);
        verify(events, times(2)).publishEvent(published.capture());
        assertThat(published.getAllValues()).extracting(e -> e.latest().lat()).containsExactly(50.2, 50.2);
    }

    @Test
    void skipsPointsRecordedBeforeMemberJoined() {
        givenActive(membership(Instant.parse("2026-11-14T11:00:00Z"), Instant.parse("2026-11-14T12:00:15Z")));

        service.ingest(userId, List.of(
                point(50.1, Instant.parse("2026-11-14T12:00:10Z")),
                point(50.2, Instant.parse("2026-11-14T12:00:20Z"))));

        assertThat(savedCount()).isEqualTo(1);
        verify(evaluator).evaluate(any(), eq(50.2), anyDouble(), anyFloat());
    }
}
