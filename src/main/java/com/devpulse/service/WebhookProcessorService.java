package com.devpulse.service;

import com.devpulse.model.entity.RawEvent;
import com.devpulse.model.payload.PullRequestEventPayload;
import com.devpulse.model.payload.PushEventPayload;
import com.devpulse.repository.RawEventRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookProcessorService {

    private final RawEventRepository rawEventRepository;
    private final ObjectMapper objectMapper;

    private static final String EVENT_PUSH = "push";
    private static final String EVENT_PULL_REQUEST = "pull_request";

    @Transactional
    public void processWebhook(String eventType,
                               String deliveryId,
                               String rawPayload) {

        log.info("Processing webhook - type: {}, deliveryId: {}",
                eventType, deliveryId);

        if (isDuplicate(deliveryId)) {
            log.warn("Duplicate webhook received - deliveryId: {}", deliveryId);
            return;
        }

        RawEvent event = switch (eventType) {
            case EVENT_PUSH -> handlePushEvent(rawPayload, deliveryId);
            case EVENT_PULL_REQUEST -> handlePullRequestEvent(rawPayload, deliveryId);
            default -> {
                log.info("Ignoring unhandled event type: {}", eventType);
                yield null;
            }
        };

        if (event != null) {
            rawEventRepository.save(event);
            log.info("Saved raw event - type: {}, repo: {}, actor: {}",
                    event.getEventType(),
                    event.getRepoName(),
                    event.getActor());
        }
    }

    private boolean isDuplicate(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) {
            return false;
        }
        Optional<RawEvent> existing =
                rawEventRepository.findByDeliveryId(deliveryId);
        return existing.isPresent();
    }

    private RawEvent handlePushEvent(String rawPayload, String deliveryId) {
        try {
            PushEventPayload payload = objectMapper
                    .readValue(rawPayload, PushEventPayload.class);

            String branch = extractBranch(payload.getRef());
            String repoName = payload.getRepository() != null
                    ? payload.getRepository().getFullName()
                    : "unknown";
            String actor = payload.getPusher() != null
                    ? payload.getPusher().getName()
                    : "unknown";

            return RawEvent.builder()
                    .eventType(EVENT_PUSH)
                    .repoName(repoName)
                    .actor(actor)
                    .branch(branch)
                    .deliveryId(deliveryId)
                    .payloadJson(rawPayload)
                    .receivedAt(LocalDateTime.now())
                    .processed(false)
                    .build();

        } catch (JacksonException e) {
            log.error("Failed to parse push event payload", e);
            return null;
        }
    }

    private RawEvent handlePullRequestEvent(String rawPayload,
                                            String deliveryId) {
        try {
            PullRequestEventPayload payload = objectMapper
                    .readValue(rawPayload, PullRequestEventPayload.class);

            String repoName = payload.getRepository() != null
                    ? payload.getRepository().getFullName()
                    : "unknown";
            String actor = payload.getSender() != null
                    ? payload.getSender().getLogin()
                    : "unknown";
            String branch = payload.getPullRequest() != null
                    && payload.getPullRequest().getHead() != null
                    ? payload.getPullRequest().getHead().getRef()
                    : null;

            return RawEvent.builder()
                    .eventType(EVENT_PULL_REQUEST)
                    .repoName(repoName)
                    .actor(actor)
                    .branch(branch)
                    .deliveryId(deliveryId)
                    .payloadJson(rawPayload)
                    .receivedAt(LocalDateTime.now())
                    .processed(false)
                    .build();

        } catch (JacksonException e) {
            log.error("Failed to parse pull request event payload", e);
            return null;
        }
    }

    private String extractBranch(String ref) {
        if (ref == null) return null;
        return ref.replace("refs/heads/", "");
    }
}