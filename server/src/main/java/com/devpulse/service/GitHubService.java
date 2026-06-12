package com.devpulse.service;

import com.devpulse.model.entity.ConnectedRepo;
import com.devpulse.repository.ConnectedRepoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitHubService {

    private final ConnectedRepoRepository connectedRepoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public List<?> getAvailableRepos(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
                "https://api.github.com/user/repos?sort=updated&per_page=50",
                HttpMethod.GET,
                entity,
                List.class
        );
        return response.getBody();
    }

    public void registerWebhook(String repoFullName, String accessToken, UUID teamId) {
        String webhookSecret = UUID.randomUUID().toString();

        Map<String, Object> config = Map.of(
                "url", "https://your-ngrok-url.ngrok.app/api/webhooks/github", // Update this for local testing
                "content_type", "json",
                "secret", webhookSecret
        );

        Map<String, Object> body = Map.of(
                "name", "web",
                "active", true,
                "events", List.of("push", "pull_request"),
                "config", config
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.github.com/repos/" + repoFullName + "/hooks",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Long webhookId = ((Number) response.getBody().get("id")).longValue();

        ConnectedRepo connectedRepo = ConnectedRepo.builder()
                .teamId(teamId)
                .repoFullName(repoFullName)
                .webhookId(webhookId)
                .webhookSecret(webhookSecret)
                .active(true)
                .build();

        connectedRepoRepository.save(connectedRepo);
        log.info("Webhook registered for repo: {} with ID: {}", repoFullName, webhookId);
    }
}