package com.rendezvous.geofence.service;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.common.geo.GeoPoints;
import com.rendezvous.geofence.dto.GeofenceRequest;
import com.rendezvous.geofence.event.GeofenceChangedEvent;
import com.rendezvous.room.domain.Geofence;
import com.rendezvous.room.domain.RoomPermissions;
import com.rendezvous.room.domain.RoomStatus;
import com.rendezvous.room.dto.GeofenceResponse;
import com.rendezvous.room.repository.GeofenceRepository;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.service.MemberContext;
import com.rendezvous.room.service.RoomAccessService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GeofenceService {

    private final GeofenceRepository geofences;
    private final RoomMemberRepository members;
    private final RoomAccessService access;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public GeofenceService(GeofenceRepository geofences, RoomMemberRepository members, RoomAccessService access,
                           ApplicationEventPublisher events, Clock clock) {
        this.geofences = geofences;
        this.members = members;
        this.access = access;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public GeofenceResponse upsert(UUID roomId, UUID userId, GeofenceRequest request) {
        MemberContext context = requireManager(roomId, userId);
        Instant now = clock.instant();
        Point center = GeoPoints.point(request.lat(), request.lon());
        Geofence geofence = geofences.findByRoomId(roomId).orElse(null);
        if (geofence == null) {
            geofences.save(new Geofence(context.room(), center, request.radiusM(), now));
        } else {
            geofence.moveTo(center, request.radiusM(), now);
        }
        changed(roomId);
        return new GeofenceResponse(request.lat(), request.lon(), request.radiusM());
    }

    @Transactional
    public void delete(UUID roomId, UUID userId) {
        requireManager(roomId, userId);
        geofences.deleteByRoomId(roomId);
        changed(roomId);
    }

    private MemberContext requireManager(UUID roomId, UUID userId) {
        MemberContext context = access.requireMember(roomId, userId);
        if (!RoomPermissions.canManageGeofence(context.role())) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        if (context.room().statusAt(clock.instant()) == RoomStatus.ENDED) {
            throw new ApiException(ErrorCode.ROOM_ENDED);
        }
        return context;
    }

    private void changed(UUID roomId) {
        geofences.flush();
        members.resetGeofenceStates(roomId);
        events.publishEvent(new GeofenceChangedEvent(roomId));
    }
}
