package com.rendezvous.room.service;

import com.rendezvous.room.domain.MemberRef;
import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.RoomMember;
import com.rendezvous.room.domain.VisibilityPolicy;
import com.rendezvous.room.repository.RoomMemberRepository;
import com.rendezvous.room.repository.RoomRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomAudienceService {

    private final RoomRepository rooms;
    private final RoomMemberRepository members;

    public RoomAudienceService(RoomRepository rooms, RoomMemberRepository members) {
        this.rooms = rooms;
        this.members = members;
    }

    public Set<UUID> activeMemberIds(UUID roomId) {
        return activeMembers(roomId).stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());
    }

    public Set<UUID> guardianIds(UUID roomId) {
        return activeMembers(roomId).stream()
                .filter(m -> m.getRole().isGuardian())
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());
    }

    public Set<UUID> positionViewerIds(UUID roomId, UUID subjectUserId) {
        List<RoomMember> active = activeMembers(roomId);
        MemberRef subject = active.stream()
                .filter(m -> m.getUser().getId().equals(subjectUserId))
                .map(RoomMember::ref)
                .findFirst()
                .orElse(null);
        if (subject == null) {
            return Set.of();
        }
        return rooms.findById(roomId)
                .map(room -> active.stream()
                        .filter(m -> !m.getUser().getId().equals(subjectUserId))
                        .filter(m -> VisibilityPolicy.canSeePosition(room.getVisibilityMode(), m.ref(), subject))
                        .map(m -> m.getUser().getId())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private List<RoomMember> activeMembers(UUID roomId) {
        return members.findByRoomIdAndStatus(roomId, MemberStatus.ACTIVE);
    }
}
