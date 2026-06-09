package com.devpulse.repository;

import com.devpulse.model.entity.WeeklyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeeklyMetricsRepository extends JpaRepository<WeeklyMetrics, UUID> {

    Optional<WeeklyMetrics> findByWeekStart(LocalDate weekStart);

    List<WeeklyMetrics> findByWeekStartAfter(LocalDate since);

    boolean existsByWeekStart(LocalDate weekStart);
}