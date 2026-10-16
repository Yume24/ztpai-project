package com.rendezvous.room.repository;

import com.rendezvous.room.domain.Room;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {

    boolean existsByCode(String code);

    Optional<Room> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.code = :code")
    Optional<Room> findByCodeForUpdate(@Param("code") String code);

    @Query("""
            select r from Room r
            where r.startsAt <= :now and r.startNotifiedAt is null
              and r.closedAt is null and r.endsAt > :now
            """)
    List<Room> findDueToStart(@Param("now") Instant now);

    @Query("""
            select r from Room r
            where (r.endsAt <= :now or r.closedAt is not null) and r.endProcessedAt is null
            order by r.id
            """)
    List<Room> findDueToEnd(@Param("now") Instant now);

    @Query("""
            select r from Room r
            where r.owner.id = :ownerId and r.closedAt is null and r.endsAt > :now
            order by r.id
            """)
    List<Room> findOpenOwnedBy(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select r from Room r
            where r.closedAt is null and r.startsAt <= :now and r.endsAt > :now and r.endProcessedAt is null
              and exists (select 1 from RoomMember m
                          where m.room = r and m.user.id = :userId
                            and m.status = com.rendezvous.room.domain.MemberStatus.ACTIVE)
            order by r.id
            """)
    List<Room> findActiveByMemberForShare(@Param("userId") UUID userId, @Param("now") Instant now);
}
