package com.devpulse.repository;
import com.devpulse.model.entity.RawEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RawEventRepository extends JpaRepository<RawEvent, Long> {
    Optional<RawEvent> findByDeliveryId(String deliveryId);
    List<RawEvent> findByProcessedFalseAndReceivedAtAfter(LocalDateTime since);

    @Modifying
    @Query("update RawEvent r set r.processed = true where r.id IN :ids")
    void markAsProcessed(@Param("ids")List<UUID> ids);
}
