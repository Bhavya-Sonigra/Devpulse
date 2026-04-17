package com.devpulse.scheduler;

import com.devpulse.model.entity.DeliveryLog;
import com.devpulse.model.entity.WeeklyMetrics;
import com.devpulse.repository.WeeklyMetricsRepository;
import com.devpulse.service.GeminiService;
import com.devpulse.service.SlackDeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportSchedulerTest {

    @Mock
    private WeeklyMetricsRepository weeklyMetricsRepository;

    @Mock
    private GeminiService geminiService;

    @Mock
    private SlackDeliveryService slackDeliveryService;

    @InjectMocks
    private ReportScheduler reportScheduler;

    @Test
    void noMetrics_shouldReturnNoMetrics() {
        when(weeklyMetricsRepository.findByWeekStartAfter(any()))
                .thenReturn(List.of());

        ReportScheduler.ReportRunResult result = reportScheduler.runWeeklyReport();

        assertEquals(ReportScheduler.ReportRunStatus.NO_METRICS, result.status());
        assertTrue(result.successLike());
        verifyNoInteractions(geminiService, slackDeliveryService);
    }

    @Test
    void emptyGeminiReport_shouldUseFallbackAndDeliver() {
        WeeklyMetrics metrics = sampleMetrics();
        DeliveryLog successLog = DeliveryLog.builder()
                .status("SUCCESS")
                .build();

        when(weeklyMetricsRepository.findByWeekStartAfter(any()))
                .thenReturn(List.of(metrics));
        when(geminiService.generateReport(metrics)).thenReturn("   ");
        when(slackDeliveryService.deliver(any(), eq(metrics.getWeekStart())))
                .thenReturn(successLog);

        ReportScheduler.ReportRunResult result = reportScheduler.runWeeklyReport();

        assertEquals(ReportScheduler.ReportRunStatus.SUCCESS, result.status());
        verify(slackDeliveryService).deliver(
                argThat(report -> report != null
                        && report.contains("Weekly update")
                        && report.contains(String.valueOf(metrics.getTotalCommits()))),
                eq(metrics.getWeekStart()));
    }

    @Test
    void failedSlackDelivery_shouldReturnDeliveryFailed() {
        WeeklyMetrics metrics = sampleMetrics();
        DeliveryLog failedLog = DeliveryLog.builder()
                .status("FAILED")
                .errorMessage("HTTP 404 NOT_FOUND")
                .build();

        when(weeklyMetricsRepository.findByWeekStartAfter(any()))
                .thenReturn(List.of(metrics));
        when(geminiService.generateReport(metrics)).thenReturn("weekly summary");
        when(slackDeliveryService.deliver("weekly summary", metrics.getWeekStart()))
                .thenReturn(failedLog);

        ReportScheduler.ReportRunResult result = reportScheduler.runWeeklyReport();

        assertEquals(ReportScheduler.ReportRunStatus.DELIVERY_FAILED, result.status());
    }

    @Test
    void successfulDelivery_shouldReturnSuccess() {
        WeeklyMetrics metrics = sampleMetrics();
        DeliveryLog successLog = DeliveryLog.builder()
                .status("SUCCESS")
                .build();

        when(weeklyMetricsRepository.findByWeekStartAfter(any()))
                .thenReturn(List.of(metrics));
        when(geminiService.generateReport(metrics)).thenReturn("weekly summary");
        when(slackDeliveryService.deliver("weekly summary", metrics.getWeekStart()))
                .thenReturn(successLog);

        ReportScheduler.ReportRunResult result = reportScheduler.runWeeklyReport();

        assertEquals(ReportScheduler.ReportRunStatus.SUCCESS, result.status());
        assertTrue(result.successLike());
    }

    private WeeklyMetrics sampleMetrics() {
        return WeeklyMetrics.builder()
                .weekStart(LocalDate.now().minusDays(4))
                .build();
    }
}
