package com.rendezvous.room.repository;

import com.rendezvous.room.domain.MemberStatus;
import com.rendezvous.room.domain.RoomMember;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

    List<RoomMember> findByRoomId(UUID roomId);

    List<RoomMember> findByRoomIdAndStatus(UUID roomId, MemberStatus status);

    long countByRoomIdAndStatus(UUID roomId, MemberStatus status);

    @Query("""
            select m from RoomMember m join fetch m.room r
            where m.user.id = :userId
              and m.status = com.rendezvous.room.domain.MemberStatus.ACTIVE
              and r.closedAt is null and r.endsAt > :now
            """)
    List<RoomMember> findActiveInOpenRooms(@Param("userId") UUID userId, @Param("now") Instant now);

    List<RoomMember> findByRoomIdInAndUserIdAndStatus(Collection<UUID> roomIds, UUID userId, MemberStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update RoomMember m set m.geofenceState = com.rendezvous.room.domain.GeofenceState.UNKNOWN where m.room.id = :roomId")
    int resetGeofenceStates(@Param("roomId") UUID roomId);
}
