package com.devpulse.controller;

import com.devpulse.model.entity.DeliveryLog;
import com.devpulse.model.entity.RawEvent;
import com.devpulse.model.entity.WeeklyMetrics;
import com.devpulse.repository.DeliveryLogRepository;
import com.devpulse.repository.RawEventRepository;
import com.devpulse.repository.WeeklyMetricsRepository;
import com.devpulse.scheduler.AnalysisScheduler;
import com.devpulse.scheduler.ReportScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
    public ResponseEntity<WeeklyMetrics> getLatestMetrics() {
        List<WeeklyMetrics> metrics = weeklyMetricsRepository
                .findByWeekStartAfter(LocalDate.now().minusDays(7));

        if (metrics.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics.get(0));
    }

    @GetMapping("/metrics/history")
    public ResponseEntity<List<WeeklyMetrics>> getMetricsHistory() {
        List<WeeklyMetrics> all = weeklyMetricsRepository
                .findByWeekStartAfter(LocalDate.now().minusDays(90));
        return ResponseEntity.ok(all);
    }

    @GetMapping("/events/recent")
    public ResponseEntity<List<RawEvent>> getRecentEvents() {
        List<RawEvent> events = rawEventRepository
                .findByProcessedFalseAndReceivedAtAfter(
                        java.time.LocalDateTime.now().minusHours(24));
        return ResponseEntity.ok(events);
    }

    @GetMapping("/deliveries")
    public ResponseEntity<List<DeliveryLog>> getDeliveries() {
        List<DeliveryLog> logs = deliveryLogRepository
                .findByStatus("SUCCESS");
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/report/trigger")
    public ResponseEntity<String> triggerReport() {
        log.info("Manual report trigger requested");
        try {
            reportScheduler.runWeeklyReport();
            return ResponseEntity.ok("Report triggered successfully");
        } catch (Exception e) {
            log.error("Manual report trigger failed", e);
            return ResponseEntity.internalServerError()
                    .body("Report trigger failed: " + e.getMessage());
        }
    }

    @PostMapping("/analysis/trigger")
    public ResponseEntity<String> triggerAnalysis() {
        log.info("Manual analysis trigger requested");
        try {
            analysisScheduler.runNightlyAnalysis();
            return ResponseEntity.ok("Analysis triggered successfully");
        } catch (Exception e) {
            log.error("Manual analysis trigger failed", e);
            return ResponseEntity.internalServerError()
                    .body("Analysis trigger failed: " + e.getMessage());
        }
    }
}