package com.devpulse.controller;

import com.devpulse.config.AppConfig;
import com.devpulse.model.entity.SlackConnection;
import com.devpulse.repository.SlackConnectionRepository;
import com.devpulse.security.DevPulseUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/slack")
@RequiredArgsConstructor
@Slf4j
public class SlackController {

    private final AppConfig appConfig;
    private final SlackConnectionRepository slackConnectionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/auth")
    public ResponseEntity<?> getAuthUrl(@AuthenticationPrincipal DevPulseUserDetails userDetails) {
        String url = "https://slack.com/oauth/v2/authorize?client_id=" + appConfig.getSlackClientId()
                + "&scope=channels:read,chat:write"
                + "&redirect_uri=http://localhost:8080/api/slack/callback"
                + "&state=" + userDetails.getTeamId().toString();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String teamIdStr) {
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", appConfig.getSlackClientId());
            body.add("client_secret", appConfig.getSlackClientSecret());
            body.add("code", code);
            body.add("redirect_uri", "http://localhost:8080/api/slack/callback");

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://slack.com/api/oauth.v2.access",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<String, Object> data = response.getBody();
            if (data == null || !Boolean.TRUE.equals(data.get("ok"))) {
                log.error("Slack OAuth failed: {}", data);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Slack OAuth failed");
            }

            String accessToken = (String) data.get("access_token");
            Map<String, Object> team = (Map<String, Object>) data.get("team");
            String workspaceId = (String) team.get("id");
            Map<String, Object> incomingWebhook = (Map<String, Object>) data.get("incoming_webhook");
            String channelId = incomingWebhook != null ? (String) incomingWebhook.get("channel_id") : "";
            String channel = incomingWebhook != null ? (String) incomingWebhook.get("channel") : "";

            UUID teamId = UUID.fromString(teamIdStr);

            Optional<SlackConnection> existing = slackConnectionRepository.findByTeamId(teamId);
            SlackConnection connection = existing.orElse(new SlackConnection());
            connection.setTeamId(teamId);
            connection.setWorkspaceId(workspaceId);
            connection.setBotToken(accessToken);
            connection.setChannelId(channelId);
            connection.setChannelName(channel);

            slackConnectionRepository.save(connection);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("http://localhost:3000/setup"))
                    .build();

        } catch (Exception e) {
            log.error("Error processing Slack callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing callback");
        }
    }

    @GetMapping("/channels")
    public ResponseEntity<?> getChannels(@AuthenticationPrincipal DevPulseUserDetails userDetails) {
        Optional<SlackConnection> connection = slackConnectionRepository.findByTeamId(userDetails.getTeamId());
        if (connection.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(connection.get().getBotToken());

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://slack.com/api/conversations.list?types=public_channel",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        Map<String, Object> data = response.getBody();
        if (data == null || !Boolean.TRUE.equals(data.get("ok"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to fetch channels");
        }

        return ResponseEntity.ok(data.get("channels"));
    }
}
