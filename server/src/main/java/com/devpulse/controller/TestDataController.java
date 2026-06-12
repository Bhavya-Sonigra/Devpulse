package com.devpulse.controller;

import com.devpulse.model.entity.RawEvent;
import com.devpulse.repository.RawEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Test endpoint for generating sample GitHub events in development.
 * This helps test the metrics computation flow without waiting for real webhooks.
 * WARNING: Only use in development/testing environments!
 */
@RestController
@RequestMapping("/api/test")
@Slf4j
@RequiredArgsConstructor
public class TestDataController {

    private final RawEventRepository rawEventRepository;

    @PostMapping("/generate-sample-events")
    public ResponseEntity<Map<String, Object>> generateSampleEvents() {
        log.info("Generating sample GitHub events for testing");
        
        List<RawEvent> events = new ArrayList<>();
        
        // Generate 10 sample push events
        String[] developers = {"alice", "bob", "charlie", "diana"};
        String[] files = {"src/main/java/App.java", "src/test/AppTest.java", "README.md", "pom.xml"};
        
        for (int i = 0; i < 10; i++) {
            String developer = developers[i % developers.length];
            String commitMessage = i % 3 == 0 ? "fix: bug fix #123" : 
                                   i % 3 == 1 ? "feat: add new feature" : 
                                   "chore: update dependencies";
            
            String payload = buildPushEventPayload(developer, commitMessage, files);
            
            RawEvent event = RawEvent.builder()
                    .eventType("push")
                    .repoName("Bhavya-Sonigra/Devpulse")
                    .actor(developer)
                    .branch("main")
                    .deliveryId("test-push-" + UUID.randomUUID())
                    .payloadJson(payload)
                    .receivedAt(LocalDateTime.now().minusHours((long)(Math.random() * 24)))
                    .processed(false)
                    .build();
            
            events.add(event);
        }
        
        // Generate 5 sample PR events
        for (int i = 0; i < 5; i++) {
            String developer = developers[i % developers.length];
            String action = i % 2 == 0 ? "opened" : "closed";
            String payload = buildPullRequestEventPayload(developer, action, i % 2 == 0);
            
            RawEvent event = RawEvent.builder()
                    .eventType("pull_request")
                    .repoName("Bhavya-Sonigra/Devpulse")
                    .actor(developer)
                    .branch("feature-" + i)
                    .deliveryId("test-pr-" + UUID.randomUUID())
                    .payloadJson(payload)
                    .receivedAt(LocalDateTime.now().minusHours((long)(Math.random() * 24)))
                    .processed(false)
                    .build();
            
            events.add(event);
        }
        
        // Save all events
        List<RawEvent> saved = rawEventRepository.saveAll(events);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Generated " + saved.size() + " sample events");
        response.put("pushEvents", 10);
        response.put("prEvents", 5);
        response.put("note", "Run /api/analysis/trigger to compute metrics from these events");
        
        log.info("Successfully generated {} sample events", saved.size());
        
        return ResponseEntity.ok(response);
    }

    private String buildPushEventPayload(String author, String message, String[] files) {
        String filesList = String.join("\",\"", files);
        return """
                {
                  "ref": "refs/heads/main",
                  "repository": {
                    "full_name": "Bhavya-Sonigra/Devpulse"
                  },
                  "pusher": {
                    "name": "%s"
                  },
                  "commits": [
                    {
                      "message": "%s",
                      "modified": ["%s"],
                      "added": [],
                      "removed": []
                    }
                  ]
                }
                """.formatted(author, message, filesList);
    }

    private String buildPullRequestEventPayload(String author, String action, boolean merged) {
        String mergedAtJson = merged ? 
            ",\"merged_at\": \"2026-06-12T06:00:00Z\"" : "";
        
        return """
                {
                  "action": "%s",
                  "pull_request": {
                    "head": {
                      "ref": "feature-branch"
                    },
                    "state": "%s",
                    "created_at": "2026-06-11T10:00:00Z"%s,
                    "merged": %s
                  },
                  "repository": {
                    "full_name": "Bhavya-Sonigra/Devpulse"
                  },
                  "sender": {
                    "login": "%s"
                  }
                }
                """.formatted(action, action.equals("opened") ? "open" : "closed", mergedAtJson, merged, author);
    }

    @PostMapping("/clean-events")
    public ResponseEntity<Map<String, Object>> cleanAllEvents() {
        log.warn("Cleaning all events - this is a destructive operation");
        
        long count = rawEventRepository.count();
        rawEventRepository.deleteAll();
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Deleted " + count + " events");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events-count")
    public ResponseEntity<Map<String, Object>> getEventsCount() {
        long totalEvents = rawEventRepository.count();
        long processedEvents = rawEventRepository.countByProcessedTrue();
        long unprocessedEvents = totalEvents - processedEvents;
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", totalEvents);
        response.put("processed", processedEvents);
        response.put("unprocessed", unprocessedEvents);
        
        return ResponseEntity.ok(response);
    }
}
