package com.devpulse.scheduler;

import com.devpulse.model.entity.WeeklyMetrics;
import com.devpulse.repository.WeeklyMetricsRepository;
import com.devpulse.service.GeminiService;
import com.devpulse.service.SlackDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReportScheduler {

    private final WeeklyMetricsRepository weeklyMetricsRepository;
    private final GeminiService geminiService;
    private final SlackDeliveryService slackDeliveryService;

    @Scheduled(cron = "${devpulse.scheduling.report-cron}")
    public void runWeeklyReport() {
        log.info("Weekly report job started");

        List<WeeklyMetrics> recentMetrics = weeklyMetricsRepository
                .findByWeekStartAfter(
                        LocalDate.now().minusDays(7));

        if (recentMetrics.isEmpty()) {
            log.warn("No metrics found for the past week - skipping report");
            return;
        }

        WeeklyMetrics metrics = recentMetrics.get(0);

        String report = geminiService.generateReport(metrics);

        if (report == null || report.isBlank()) {
            log.error("Gemini returned empty report - skipping Slack delivery");
            return;
        }

        slackDeliveryService.deliver(report, metrics.getWeekStart());

        log.info("Weekly report job completed");
    }
}