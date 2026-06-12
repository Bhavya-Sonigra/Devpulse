package com.devpulse.controller;

import com.devpulse.model.entity.DeliveryLog;
import com.devpulse.model.entity.RawEvent;
import com.devpulse.model.entity.WeeklyMetrics;
import com.devpulse.repository.DeliveryLogRepository;
import com.devpulse.repository.RawEventRepository;
import com.devpulse.repository.WeeklyMetricsRepository;
import com.devpulse.scheduler.AnalysisScheduler;
import lombok.RequiredArgsConstructor;
import com.devpulse.scheduler.ReportScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class MetricsController {

    private final WeeklyMetricsRepository weeklyMetricsRepository;
    private final RawEventRepository rawEventRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final AnalysisScheduler analysisScheduler;
    private final ReportScheduler reportScheduler;

    @GetMapping("/metrics/latest")
    public ResponseEntity<WeeklyMetrics> getLatestMetrics(@RequestAttribute("teamId") String teamId) {
        UUID teamUuid = UUID.fromString(teamId);
        List<WeeklyMetrics> metrics = weeklyMetricsRepository
                .findByTeamIdAndWeekStartAfter(teamUuid, LocalDate.now().minusDays(7));
        if (metrics.isEmpty()) {
            log.info("No metrics found for team: {}", teamId);
            return ResponseEntity.noContent().build();
        }
        WeeklyMetrics latest = metrics.stream()
                .max((m1, m2) -> m1.getWeekStart().compareTo(m2.getWeekStart()))
                .orElse(null);
        return ResponseEntity.ok(latest);
    }

    @GetMapping("/metrics/history")
    public ResponseEntity<List<WeeklyMetrics>> getMetricsHistory(@RequestAttribute("teamId") String teamId) {
        UUID teamUuid = UUID.fromString(teamId);
        List<WeeklyMetrics> all = weeklyMetricsRepository
                .findByTeamIdAndWeekStartAfter(teamUuid, LocalDate.now().minusDays(90));
        log.info("Fetched {} metric records for team: {}", all.size(), teamId);
        return ResponseEntity.ok(all);
    }

    @GetMapping("/events/recent")
    public ResponseEntity<List<RawEvent>> getRecentEvents(@RequestAttribute("teamId") String teamId) {
        UUID teamUuid = UUID.fromString(teamId);
        List<RawEvent> events = rawEventRepository
                .findByTeamIdAndProcessedFalseAndReceivedAtAfter(
                        teamUuid, LocalDateTime.now().minusHours(24));
        return ResponseEntity.ok(events);
    }

    @GetMapping("/deliveries")
    public ResponseEntity<List<DeliveryLog>> getDeliveries(@RequestAttribute("teamId") String teamId) {
        // Get the 20 most recent delivery logs
        List<DeliveryLog> logs = deliveryLogRepository.findTop100ByOrderByDeliveredAtDesc();
        if (logs.size() > 20) {
            logs = logs.subList(0, 20);
        }
        log.info("Fetched {} delivery logs for team: {}", logs.size(), teamId);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/report/trigger")
    public ResponseEntity<String> triggerReport(@RequestAttribute("teamId") String teamId) {
        log.info("Manual report trigger requested for team: {}", teamId);
        try {
            reportScheduler.runWeeklyReport();
            return ResponseEntity.ok("Report triggered successfully");
        } catch (Exception e) {
            log.error("Manual report trigger failed for team: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body("Report trigger failed: " + e.getMessage());
        }
    }

    @PostMapping("/analysis/trigger")
    public ResponseEntity<String> triggerAnalysis(@RequestAttribute("teamId") String teamId) {
        log.info("Manual analysis trigger requested for team: {}", teamId);
        try {
            analysisScheduler.runNightlyAnalysis();
            return ResponseEntity.ok("Analysis triggered successfully");
        } catch (Exception e) {
            log.error("Manual analysis trigger failed for team: {}", teamId, e);
            return ResponseEntity.internalServerError()
                    .body("Analysis trigger failed: " + e.getMessage());
        }
    }
}