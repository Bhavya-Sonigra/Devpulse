package com.devpulse.service;

import com.devpulse.config.AppConfig;
import com.devpulse.model.entity.WeeklyMetrics;
import com.devpulse.model.payload.GeminiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiService {

    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public String generateReport(WeeklyMetrics metrics) {
        String apiKey = appConfig.getGeminiApiKey().trim();
        String apiUrl = appConfig.getGeminiApiUrl().trim();

        if (apiKey.isBlank() || apiKey.equals("your-gemini-key-here")) {
            log.error("Gemini API key is not configured properly");
            return null;
        }

        log.info("Calling Gemini API for week: {}", metrics.getWeekStart());
        log.debug("Using API URL: {}", apiUrl);

        String prompt = buildPrompt(metrics);
        String requestBody = buildRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String urlWithKey = apiUrl + "?key=" + apiKey;
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    urlWithKey,
                    HttpMethod.POST,
                    request,
                    String.class);

            log.info("Gemini API responded with status: {}",
                    response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                log.error("Gemini returned bad response: {} | body: {}",
                        response.getStatusCode(), response.getBody());
                return null;
            }

            log.debug("Gemini raw response: {}", response.getBody());

            GeminiResponse geminiResponse = objectMapper
                    .readValue(response.getBody(), GeminiResponse.class);

            String text = geminiResponse.extractText();

            if (text == null || text.isBlank()) {
                log.error("Gemini extractText returned empty — raw body: {}",
                        response.getBody());
                return null;
            }

            log.info("Gemini generated report — {} chars", text.length());
            return text.trim();

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildPrompt(WeeklyMetrics metrics) {
        return String.format("""
            You are writing a weekly engineering digest for a software team.
            Write exactly 2 paragraphs. Plain text only, no markdown, no bullet points.
            Be factual and professional like an internal engineering newsletter.
            Do not give advice, directives, or tell the team what to do.
            Do not use phrases like "this week we need to" or "the team should".
            No motivational language. No filler sentences. Just report the facts clearly.
            Under 120 words total.

            Paragraph 1: Summarise commit activity — total commits, who contributed and what they touched.
            Paragraph 2: Summarise PR activity — opened, merged, still open, average review time if available.

            Data:
            Week of: %s
            Total commits: %d
            Top contributor: %s
            Most changed file: %s
            Commits by person: %s
            Bug fixes: %d | New features: %d
            PRs opened: %d | PRs merged: %d | PRs still open: %d
            Avg PR review time: %s hours
            """,
                metrics.getWeekStart(),
                metrics.getTotalCommits(),
                metrics.getTopContributor() != null
                        ? metrics.getTopContributor() : "N/A",
                metrics.getMostChangedFile() != null
                        ? metrics.getMostChangedFile() : "N/A",
                metrics.getCommitsByUser() != null
                        ? metrics.getCommitsByUser() : "{}",
                metrics.getBugFixCount(),
                metrics.getFeatureCount(),
                metrics.getPrsOpened(),
                metrics.getPrsMerged(),
                metrics.getPrsStillOpen(),
                metrics.getAvgPrOpenHours() != null
                        ? metrics.getAvgPrOpenHours() : "N/A");
    }

    private String buildRequestBody(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.error("Failed to build Gemini request body", e);
            throw new RuntimeException("Could not build request", e);
        }
    }
}