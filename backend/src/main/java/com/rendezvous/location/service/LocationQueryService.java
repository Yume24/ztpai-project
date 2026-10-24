package com.rendezvous.location.service;

import com.rendezvous.common.error.ApiException;
import com.rendezvous.common.error.ErrorCode;
import com.rendezvous.common.geo.GeoPoints;
import com.rendezvous.location.dto.PositionResponse;
import com.rendezvous.location.dto.TrailPointResponse;
import com.rendezvous.location.repository.LocationPointRepository;
import com.rendezvous.room.domain.MemberRef;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.Room;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.VisibilityPolicy;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.service.MemberContext;
import com.rendezvous.room.service.RoomAccessService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationQueryService {

    private final RoomAccessService access;
    private final RoomMemberRepository members;
    private final LocationPointRepository points;

    public LocationQueryService(RoomAccessService access, RoomMemberRepository members,
                                LocationPointRepository points) {
        this.access = access;
        this.members = members;
        this.points = points;
    }

    @Transactional(readOnly = true)
    public List<PositionResponse> latest(UUID roomId, UUID viewerId) {
        MemberContext context = access.requireMember(roomId, viewerId);
        Room room = context.room();
        return points.findLatestByRoomId(roomId).stream()
                .filter(row -> VisibilityPolicy.canSeePosition(room.getVisibilityMode(),
                        context.member().ref(), new MemberRef(row.getUserId(), row.getRole())))
                .map(row -> new PositionResponse(row.getUserId(), row.getLat(), row.getLon(),
                        row.getAccuracyM(), row.getRecordedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrailPointResponse> trail(UUID roomId, UUID viewerId, UUID subjectUserId) {
        MemberContext context = access.requireMember(roomId, viewerId);
        Room room = context.room();
        RoomMember subject = members.findByRoomIdAndUserId(roomId, subjectUserId)
                .filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
        if (!VisibilityPolicy.canSeeTrail(room.getVisibilityMode(), room.isMembersSeeTrails(),
                context.member().ref(), subject.ref())) {
            throw new ApiException(ErrorCode.ROOM_ACTION_FORBIDDEN);
        }
        return points.findByMemberIdOrderByRecordedAtAsc(subject.getId()).stream()
                .map(p -> new TrailPointResponse(GeoPoints.lat(p.getPosition()), GeoPoints.lon(p.getPosition()),
                        p.getRecordedAt()))
                .toList();
    }
}
