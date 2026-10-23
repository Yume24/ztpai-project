package com.rendezvous.room.repository;

import com.rendezvous.room.domain.Geofence;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {

    Optional<Geofence> findByRoomId(UUID roomId);

    void deleteByRoomId(UUID roomId);

    @Query(value = """
            select ST_DWithin(g.center::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, g.radius_m)
            from geofences g where g.room_id = :roomId
            """, nativeQuery = true)
    Optional<Boolean> contains(@Param("roomId") UUID roomId, @Param("lat") double lat, @Param("lon") double lon);
}
