package com.devpulse.repository;

import com.devpulse.model.entity.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, UUID> {

    Optional<DeliveryLog> findByReportWeek(LocalDate reportWeek);

    List<DeliveryLog> findByStatus(String status);

    boolean existsByReportWeekAndStatus(LocalDate reportWeek, String status);
}