package com.devpulse.scheduler;

import com.devpulse.model.entity.DeliveryLog;
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

    public enum ReportRunStatus {
        SUCCESS,
        NO_METRICS,
        DELIVERY_FAILED
    }

    public record ReportRunResult(ReportRunStatus status, String message) {
        public boolean successLike() {
            return status == ReportRunStatus.SUCCESS
                    || status == ReportRunStatus.NO_METRICS;
        }
    }

    @Scheduled(cron = "${devpulse.scheduling.report-cron}")
    public ReportRunResult runWeeklyReport() {
        log.info("Weekly report job started");
        boolean fallbackUsed = false;

        List<WeeklyMetrics> recentMetrics = weeklyMetricsRepository
                .findByWeekStartAfter(LocalDate.now().minusDays(7));

        if (recentMetrics.isEmpty()) {
            String message = "No metrics found for the past week - skipping report";
            log.warn(message);
            return new ReportRunResult(ReportRunStatus.NO_METRICS, message);
        }

        WeeklyMetrics metrics = recentMetrics.get(0);
        String report = geminiService.generateReport(metrics);

        if (report == null || report.isBlank()) {
            log.warn("Gemini returned empty report - using fallback summary");
            report = buildFallbackReport(metrics);
            fallbackUsed = true;
        }

        DeliveryLog deliveryLog = slackDeliveryService.deliver(report, metrics.getWeekStart());

        if (deliveryLog == null) {
            String message = "Delivery attempt was made but log persistence failed";
            log.error(message);
            return new ReportRunResult(ReportRunStatus.DELIVERY_FAILED, message);
        }

        if (!"SUCCESS".equalsIgnoreCase(deliveryLog.getStatus())) {
            String message = "Slack delivery failed: "
                    + (deliveryLog.getErrorMessage() != null
                    ? deliveryLog.getErrorMessage()
                    : "unknown error");
            log.error(message);
            return new ReportRunResult(ReportRunStatus.DELIVERY_FAILED, message);
        }

        String message = fallbackUsed
                ? "Weekly report delivered using fallback summary"
                : "Weekly report delivered successfully";
        log.info(message);
        return new ReportRunResult(ReportRunStatus.SUCCESS, message);
    }

    private String buildFallbackReport(WeeklyMetrics metrics) {
        return String.format(
                "Weekly update (%s): %d commits, %d PRs opened, %d PRs merged, %d PRs still open. "
                        + "Top contributor: %s. Bug fixes: %d. Features: %d. Most changed file: %s.",
                metrics.getWeekStart(),
                metrics.getTotalCommits(),
                metrics.getPrsOpened(),
                metrics.getPrsMerged(),
                metrics.getPrsStillOpen(),
                metrics.getTopContributor() != null ? metrics.getTopContributor() : "N/A",
                metrics.getBugFixCount(),
                metrics.getFeatureCount(),
                metrics.getMostChangedFile() != null ? metrics.getMostChangedFile() : "N/A");
    }
}
