package com.devpulse.scheduler;

import com.devpulse.model.entity.RawEvent;
import com.devpulse.repository.RawEventRepository;
import com.devpulse.service.MetricsComputationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnalysisScheduler {

    private final RawEventRepository rawEventRepository;
    private final MetricsComputationService metricsComputationService;

    @Scheduled(cron = "${devpulse.scheduling.analysis-cron}")
    public void runNightlyAnalysis() {
        log.info("Nightly analysis job started at {}",
                LocalDateTime.now());

        List<RawEvent> unprocessedEvents = rawEventRepository
                .findByProcessedFalseAndReceivedAtAfter(
                        LocalDateTime.now().minusHours(24));

        log.info("Found {} unprocessed events",
                unprocessedEvents.size());

        metricsComputationService.computeMetrics(unprocessedEvents);

        log.info("Nightly analysis job completed at {}",
                LocalDateTime.now());
    }
}