package com.rendezvous.user.repository;

import com.rendezvous.user.domain.PushToken;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

    Optional<PushToken> findByToken(String token);

    List<PushToken> findByUserIdIn(Collection<UUID> userIds);

    @Modifying
    @Query("delete from PushToken p where p.token in :tokens")
    void deleteByTokenIn(@Param("tokens") Collection<String> tokens);

    @Modifying
    @Query("delete from PushToken p where p.user.id = :userId and p.token = :token")
    void deleteByUserIdAndToken(@Param("userId") UUID userId, @Param("token") String token);
}
