package com.rendezvous.location.repository;

import com.rendezvous.location.domain.LocationPoint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocationPointRepository extends JpaRepository<LocationPoint, Long> {

    @Modifying
    @Query("delete from LocationPoint p where p.member.id in (select m.id from RoomMember m where m.room.id = :roomId)")
    int deleteByRoomId(@Param("roomId") UUID roomId);

    @Query(value = """
            select distinct on (p.member_id)
                   m.user_id as userId, m.role as role, ST_Y(p.position) as lat, ST_X(p.position) as lon,
                   p.accuracy_m as accuracyM, p.recorded_at as recordedAt
            from location_points p
            join room_members m on m.id = p.member_id
            where m.room_id = :roomId and m.status = 'ACTIVE'
            order by p.member_id, p.recorded_at desc
            """, nativeQuery = true)
    List<LatestPositionRow> findLatestByRoomId(@Param("roomId") UUID roomId);

    List<LocationPoint> findByMemberIdOrderByRecordedAtAsc(UUID memberId);
}
