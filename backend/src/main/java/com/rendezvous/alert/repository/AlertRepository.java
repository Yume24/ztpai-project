package com.rendezvous.alert.repository;

import com.rendezvous.alert.domain.Alert;
import com.rendezvous.alert.domain.AlertType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @EntityGraph(attributePaths = "subject")
    Page<Alert> findByRoomId(UUID roomId, Pageable pageable);

    @EntityGraph(attributePaths = "subject")
    Page<Alert> findByRoomIdAndType(UUID roomId, AlertType type, Pageable pageable);
}
