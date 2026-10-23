package com.rendezvous.location.service;

import com.rendezvous.common.geo.GeoPoints;
import com.rendezvous.geofence.service.GeofenceEvaluator;
import com.rendezvous.location.domain.LocationPoint;
import com.rendezvous.location.dto.LocationPointRequest;
import com.rendezvous.location.dto.PositionResponse;
import com.rendezvous.location.event.LocationsRecordedEvent;
import com.rendezvous.location.repository.LocationPointRepository;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationIngestService {

    static final Duration MAX_AGE = Duration.ofMinutes(15);
    static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(1);

    private final RoomRepository rooms;
    private final RoomMemberRepository members;
    private final LocationPointRepository points;
    private final GeofenceEvaluator geofenceEvaluator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LocationIngestService(RoomRepository rooms, RoomMemberRepository members, LocationPointRepository points,
                                 GeofenceEvaluator geofenceEvaluator, ApplicationEventPublisher events,
                                 Clock clock) {
        this.rooms = rooms;
        this.members = members;
        this.points = points;
        this.geofenceEvaluator = geofenceEvaluator;
        this.events = events;
        this.clock = clock;
    }

    // Share-locks the rooms so a concurrent room end either waits for this batch or makes it skip the room.
    @Transactional
    public int ingest(UUID userId, List<LocationPointRequest> batch) {
        Instant now = clock.instant();
        Instant oldest = now.minus(MAX_AGE);
        Instant latest = now.plus(MAX_FUTURE_SKEW);
        List<LocationPointRequest> fresh = batch.stream()
                .filter(p -> !p.recordedAt().isBefore(oldest) && !p.recordedAt().isAfter(latest))
                .toList();

        List<UUID> roomIds = rooms.findActiveByMemberForShare(userId, now).stream().map(Room::getId).toList();
        if (roomIds.isEmpty()) {
            return 0;
        }
        List<RoomMember> memberships = members.findByRoomIdInAndUserIdAndStatus(roomIds, userId, MemberStatus.ACTIVE);
        for (RoomMember member : memberships) {
            record(member, fresh, now);
        }
        return memberships.size();
    }

    private void record(RoomMember member, List<LocationPointRequest> fresh, Instant now) {
        Room room = member.getRoom();
        Instant from = sharingSince(member);
        List<LocationPointRequest> inWindow = fresh.stream()
                .filter(p -> !p.recordedAt().isBefore(from) && p.recordedAt().isBefore(room.effectiveEnd()))
                .toList();
        if (inWindow.isEmpty()) {
            return;
        }
        points.saveAll(inWindow.stream()
                .map(p -> new LocationPoint(member, GeoPoints.point(p.lat(), p.lon()), p.accuracyM(),
                        p.recordedAt(), now))
                .toList());
        LocationPointRequest newest = inWindow.stream()
                .max(Comparator.comparing(LocationPointRequest::recordedAt))
                .orElseThrow();
        geofenceEvaluator.evaluate(member, newest.lat(), newest.lon(), newest.accuracyM());
        events.publishEvent(new LocationsRecordedEvent(room.getId(), new PositionResponse(
                member.getUser().getId(), newest.lat(), newest.lon(), newest.accuracyM(), newest.recordedAt())));
    }

    private static Instant sharingSince(RoomMember member) {
        Instant startsAt = member.getRoom().getStartsAt();
        return member.getJoinedAt().isAfter(startsAt) ? member.getJoinedAt() : startsAt;
    }
}
