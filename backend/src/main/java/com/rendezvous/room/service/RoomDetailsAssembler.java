package com.rendezvous.room.service;

import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.dto.GeofenceResponse;
import com.rendezvous.room.dto.OwnerResponse;
import com.rendezvous.room.dto.RoomDetailsResponse;
import com.rendezvous.common.geo.GeoPoints;
import com.rendezvous.room.repository.GeofenceRepository;
import com.rendezvous.room.repository.RoomMemberRepository;
import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class RoomDetailsAssembler {

    private final RoomMemberRepository members;
    private final GeofenceRepository geofences;
    private final Clock clock;

    public RoomDetailsAssembler(RoomMemberRepository members, GeofenceRepository geofences, Clock clock) {
        this.members = members;
        this.geofences = geofences;
        this.clock = clock;
    }

    public RoomDetailsResponse toDetails(Room room, RoomMember viewer) {
        return new RoomDetailsResponse(
                room.getId(),
                room.getName(),
                room.getCode(),
                room.statusAt(clock.instant()),
                room.getStartsAt(),
                room.getEndsAt(),
                room.getClosedAt(),
                room.getVisibilityMode(),
                room.isMembersSeeTrails(),
                room.getVersion(),
                viewer.getRole(),
                members.countByRoomIdAndStatus(room.getId(), MemberStatus.ACTIVE),
                OwnerResponse.from(room.getOwner()),
                geofences.findByRoomId(room.getId())
                        .map(g -> new GeofenceResponse(GeoPoints.lat(g.getCenter()), GeoPoints.lon(g.getCenter()),
                                g.getRadiusM()))
                        .orElse(null));
    }
}
