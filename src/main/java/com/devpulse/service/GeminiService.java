package com.devpulse.service;

import com.devpulse.config.AppConfig;
import com.devpulse.model.entity.WeeklyMetrics;
import com.devpulse.model.payload.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

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
        log.info("Generating report via Gemini for week: {}",
                metrics.getWeekStart());

        String prompt = buildPrompt(metrics);
        String requestBody = buildRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String urlWithKey = appConfig.getGeminiApiUrl()
                + "?key=" + appConfig.getGeminiApiKey();

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    urlWithKey,
                    HttpMethod.POST,
                    request,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {
                log.error("Gemini API returned non-200: {}",
                        response.getStatusCode());
                return null;
            }

            GeminiResponse geminiResponse = objectMapper
                    .readValue(response.getBody(), GeminiResponse.class);

            String text = geminiResponse.extractText();

            if (text == null || text.isBlank()) {
                log.error("Gemini response contained no text");
                return null;
            }

            log.info("Report generated successfully - {} chars",
                    text.length());
            return text;

        } catch (Exception e) {
            log.error("Failed to call Gemini API", e);
            return null;
        }
    }

    private String buildPrompt(WeeklyMetrics metrics) {
        return String.format("""
                You are writing a Monday morning team update for a software development team.
                Write a concise, friendly message based on this data.
                Highlight the biggest wins, flag anything concerning, end with encouragement.
                Keep it under 200 words. Use plain text, no markdown, no bullet points.
                                
                Week of: %s
                Total commits: %d
                PRs opened: %d
                PRs merged: %d
                PRs still open: %d
                Average PR review time: %s hours
                Top contributor: %s
                Bug fixes: %d
                New features: %d
                Most changed file: %s
                Commits by person: %s
                """,
                metrics.getWeekStart(),
                metrics.getTotalCommits(),
                metrics.getPrsOpened(),
                metrics.getPrsMerged(),
                metrics.getPrsStillOpen(),
                metrics.getAvgPrOpenHours() != null
                        ? metrics.getAvgPrOpenHours() : "N/A",
                metrics.getTopContributor() != null
                        ? metrics.getTopContributor() : "N/A",
                metrics.getBugFixCount(),
                metrics.getFeatureCount(),
                metrics.getMostChangedFile() != null
                        ? metrics.getMostChangedFile() : "N/A",
                metrics.getCommitsByUser() != null
                        ? metrics.getCommitsByUser() : "{}");
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