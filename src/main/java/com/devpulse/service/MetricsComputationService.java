package com.devpulse.service;

import com.devpulse.model.entity.RawEvent;
import com.devpulse.model.entity.WeeklyMetrics;
import com.devpulse.model.payload.PullRequestEventPayload;
import com.devpulse.model.payload.PushEventPayload;
import com.devpulse.repository.RawEventRepository;
import com.devpulse.repository.WeeklyMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsComputationService {

    private final RawEventRepository rawEventRepository;
    private final WeeklyMetricsRepository weeklyMetricsRepository;
    private final ObjectMapper objectMapper;

    private static final String EVENT_PUSH = "push";
    private static final String EVENT_PULL_REQUEST = "pull_request";
    private static final List<String> BUG_KEYWORDS =
            List.of("fix", "bug", "patch", "hotfix", "resolve", "issue");
    private static final List<String> FEATURE_KEYWORDS =
            List.of("feat", "feature", "add", "new", "implement");

    @Transactional
    public void computeMetrics(List<RawEvent> events) {
        if (events == null || events.isEmpty()) {
            log.info("No events to process");
            return;
        }

        log.info("Computing metrics for {} events", events.size());

        LocalDate weekStart = LocalDate.now()
                .with(java.time.DayOfWeek.MONDAY);

        if (weeklyMetricsRepository.existsByWeekStart(weekStart)) {
            log.info("Metrics already computed for week: {}", weekStart);
            return;
        }

        List<RawEvent> pushEvents = filterByType(events, EVENT_PUSH);
        List<RawEvent> prEvents = filterByType(events, EVENT_PULL_REQUEST);

        int totalCommits = countTotalCommits(pushEvents);
        Map<String, Integer> commitsByUser = countCommitsByUser(pushEvents);
        String topContributor = findTopContributor(commitsByUser);
        String mostChangedFile = findMostChangedFile(pushEvents);
        int bugFixCount = countByKeywords(pushEvents, BUG_KEYWORDS);
        int featureCount = countByKeywords(pushEvents, FEATURE_KEYWORDS);

        int prsOpened = countPrsOpened(prEvents);
        int prsMerged = countPrsMerged(prEvents);
        int prsStillOpen = countPrsStillOpen(prEvents);
        Double avgPrOpenHours = computeAvgPrOpenHours(prEvents);

        WeeklyMetrics metrics = WeeklyMetrics.builder()
                .weekStart(weekStart)
                .totalCommits(totalCommits)
                .commitsByUser(mapToJson(commitsByUser))
                .topContributor(topContributor)
                .mostChangedFile(mostChangedFile)
                .bugFixCount(bugFixCount)
                .featureCount(featureCount)
                .prsOpened(prsOpened)
                .prsMerged(prsMerged)
                .prsStillOpen(prsStillOpen)
                .avgPrOpenHours(avgPrOpenHours)
                .computedAt(LocalDateTime.now())
                .build();

        weeklyMetricsRepository.save(metrics);

        markEventsProcessed(events);

        log.info("Metrics saved for week {} — {} commits, {} PRs merged",
                weekStart, totalCommits, prsMerged);
    }

    private List<RawEvent> filterByType(List<RawEvent> events,
                                        String type) {
        return events.stream()
                .filter(e -> type.equals(e.getEventType()))
                .collect(Collectors.toList());
    }

    private int countTotalCommits(List<RawEvent> pushEvents) {
        return pushEvents.stream()
                .mapToInt(event -> {
                    PushEventPayload payload = parsePayload(
                            event.getPayloadJson(),
                            PushEventPayload.class);
                    if (payload == null
                            || payload.getCommits() == null) {
                        return 0;
                    }
                    return payload.getCommits().size();
                })
                .sum();
    }

    private Map<String, Integer> countCommitsByUser(
            List<RawEvent> pushEvents) {

        Map<String, Integer> counts = new HashMap<>();
        for (RawEvent event : pushEvents) {
            String actor = event.getActor();
            if (actor != null) {
                counts.merge(actor, 1, Integer::sum);
            }
        }
        return counts;
    }

    private String findTopContributor(Map<String, Integer> commitsByUser) {
        return commitsByUser.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String findMostChangedFile(List<RawEvent> pushEvents) {
        Map<String, Integer> fileCounts = new HashMap<>();

        for (RawEvent event : pushEvents) {
            PushEventPayload payload = parsePayload(
                    event.getPayloadJson(),
                    PushEventPayload.class);
            if (payload == null || payload.getCommits() == null) {
                continue;
            }
            for (PushEventPayload.Commit commit : payload.getCommits()) {
                addFilesToCount(fileCounts, commit.getModified());
                addFilesToCount(fileCounts, commit.getAdded());
            }
        }

        return fileCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void addFilesToCount(Map<String, Integer> fileCounts,
                                 List<String> files) {
        if (files == null) return;
        for (String file : files) {
            fileCounts.merge(file, 1, Integer::sum);
        }
    }

    private int countByKeywords(List<RawEvent> pushEvents,
                                List<String> keywords) {
        int count = 0;
        for (RawEvent event : pushEvents) {
            PushEventPayload payload = parsePayload(
                    event.getPayloadJson(),
                    PushEventPayload.class);
            if (payload == null || payload.getCommits() == null) {
                continue;
            }
            for (PushEventPayload.Commit commit : payload.getCommits()) {
                if (commit.getMessage() == null) continue;
                String message = commit.getMessage().toLowerCase();
                boolean matches = keywords.stream()
                        .anyMatch(message::contains);
                if (matches) count++;
            }
        }
        return count;
    }

    private int countPrsOpened(List<RawEvent> prEvents) {
        return (int) prEvents.stream()
                .filter(event -> {
                    PullRequestEventPayload payload = parsePayload(
                            event.getPayloadJson(),
                            PullRequestEventPayload.class);
                    return payload != null
                            && "opened".equals(payload.getAction());
                })
                .count();
    }

    private int countPrsMerged(List<RawEvent> prEvents) {
        return (int) prEvents.stream()
                .filter(event -> {
                    PullRequestEventPayload payload = parsePayload(
                            event.getPayloadJson(),
                            PullRequestEventPayload.class);
                    return payload != null
                            && "closed".equals(payload.getAction())
                            && payload.getPullRequest() != null
                            && payload.getPullRequest().isMerged();
                })
                .count();
    }

    private int countPrsStillOpen(List<RawEvent> prEvents) {
        return (int) prEvents.stream()
                .filter(event -> {
                    PullRequestEventPayload payload = parsePayload(
                            event.getPayloadJson(),
                            PullRequestEventPayload.class);
                    return payload != null
                            && payload.getPullRequest() != null
                            && "open".equals(
                            payload.getPullRequest().getState());
                })
                .count();
    }

    private Double computeAvgPrOpenHours(List<RawEvent> prEvents) {
        List<Double> durations = new ArrayList<>();

        for (RawEvent event : prEvents) {
            PullRequestEventPayload payload = parsePayload(
                    event.getPayloadJson(),
                    PullRequestEventPayload.class);

            if (payload == null
                    || payload.getPullRequest() == null
                    || payload.getPullRequest().getCreatedAt() == null
                    || payload.getPullRequest().getMergedAt() == null) {
                continue;
            }

            try {
                LocalDateTime created = LocalDateTime.parse(
                        payload.getPullRequest()
                                .getCreatedAt()
                                .replace("Z", ""));
                LocalDateTime merged = LocalDateTime.parse(
                        payload.getPullRequest()
                                .getMergedAt()
                                .replace("Z", ""));

                double hours = ChronoUnit.MINUTES
                        .between(created, merged) / 60.0;
                durations.add(hours);

            } catch (Exception e) {
                log.warn("Could not parse PR timestamps", e);
            }
        }

        if (durations.isEmpty()) return null;

        double avg = durations.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return Math.round(avg * 10.0) / 10.0;
    }

    private void markEventsProcessed(List<RawEvent> events) {
        List<UUID> ids = events.stream()
                .map(RawEvent::getId)
                .collect(Collectors.toList());
        rawEventRepository.markAsProcessed(ids);
        log.debug("Marked {} events as processed", ids.size());
    }

    private <T> T parsePayload(String json, Class<T> type) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Failed to parse payload as {}: {}",
                    type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private String mapToJson(Map<String, Integer> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize map to JSON", e);
            return "{}";
        }
    }
}