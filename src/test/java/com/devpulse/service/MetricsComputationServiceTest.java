package com.devpulse.service;

import com.devpulse.model.entity.RawEvent;
import com.devpulse.model.entity.WeeklyMetrics;
import com.devpulse.repository.RawEventRepository;
import com.devpulse.repository.WeeklyMetricsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsComputationServiceTest {

    @Mock
    private RawEventRepository rawEventRepository;

    @Mock
    private WeeklyMetricsRepository weeklyMetricsRepository;

    @InjectMocks
    private MetricsComputationService metricsComputationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void emptyEventList_shouldReturnEarly() {
        metricsComputationService.computeMetrics(List.of());
        verify(weeklyMetricsRepository, never()).save(any());
    }

    @Test
    void nullEventList_shouldReturnEarly() {
        metricsComputationService.computeMetrics(null);
        verify(weeklyMetricsRepository, never()).save(any());
    }

    @Test
    void pushEvents_shouldComputeCommitCount() {
        String payload = """
                {
                  "ref": "refs/heads/main",
                  "repository": {"name": "devpulse", "full_name": "bhavya/devpulse"},
                  "pusher": {"name": "Bhavya"},
                  "commits": [
                    {"id": "abc1", "message": "feat: add auth", 
                     "timestamp": "2026-04-12T10:00:00Z",
                     "added": ["Auth.java"], "modified": [], "removed": []},
                    {"id": "abc2", "message": "fix: resolve bug",
                     "timestamp": "2026-04-12T11:00:00Z",
                     "added": [], "modified": ["Service.java"], "removed": []}
                  ]
                }
                """;

        RawEvent event = RawEvent.builder()
                .id(UUID.randomUUID())
                .eventType("push")
                .repoName("bhavya/devpulse")
                .actor("Bhavya")
                .branch("main")
                .payloadJson(payload)
                .receivedAt(LocalDateTime.now())
                .processed(false)
                .build();

        when(weeklyMetricsRepository.existsByWeekStart(any()))
                .thenReturn(false);

        ArgumentCaptor<WeeklyMetrics> captor =
                ArgumentCaptor.forClass(WeeklyMetrics.class);

        metricsComputationService.computeMetrics(List.of(event));

        verify(weeklyMetricsRepository).save(captor.capture());
        WeeklyMetrics saved = captor.getValue();

        assertEquals(2, saved.getTotalCommits());
        assertEquals(1, saved.getFeatureCount());
        assertEquals(1, saved.getBugFixCount());
        assertEquals("Bhavya", saved.getTopContributor());
    }

    @Test
    void metricsAlreadyExist_shouldSkipComputation() {
        RawEvent event = RawEvent.builder()
                .id(UUID.randomUUID())
                .eventType("push")
                .repoName("test/repo")
                .actor("Bhavya")
                .payloadJson("{}")
                .receivedAt(LocalDateTime.now())
                .processed(false)
                .build();

        when(weeklyMetricsRepository.existsByWeekStart(any()))
                .thenReturn(true);

        metricsComputationService.computeMetrics(List.of(event));

        verify(weeklyMetricsRepository, never()).save(any());
    }
}