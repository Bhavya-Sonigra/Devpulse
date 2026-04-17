package com.devpulse.service;

import com.devpulse.config.AppConfig;
import com.devpulse.model.entity.DeliveryLog;
import com.devpulse.repository.DeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlackDeliveryService {

    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final DeliveryLogRepository deliveryLogRepository;

    public DeliveryLog deliver(String report, LocalDate reportWeek) {
        log.info("Delivering report to Slack for week: {}", reportWeek);

        String status;
        String errorMessage = null;
        String webhookUrl = appConfig.getSlackWebhookUrl() == null
                ? ""
                : appConfig.getSlackWebhookUrl().trim();

        if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
            status = "FAILED";
            errorMessage = "Slack webhook URL is missing or invalid";
            log.error(errorMessage);
            return saveDeliveryLog(report, reportWeek, status, errorMessage);
        }

        try {
            String payload = "{\"text\": \""
                    + report.replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    + "\"}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request =
                    new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Report delivered to Slack successfully");
                status = "SUCCESS";
            } else {
                log.error("Slack returned non-200: {}",
                        response.getStatusCode());
                status = "FAILED";
                errorMessage = "HTTP " + response.getStatusCode();
            }

        } catch (Exception e) {
            log.error("Failed to deliver report to Slack", e);
            status = "FAILED";
            errorMessage = e.getMessage();
        }

        return saveDeliveryLog(report, reportWeek, status, errorMessage);
    }

    private DeliveryLog saveDeliveryLog(String report,
                                        LocalDate reportWeek,
                                        String status,
                                        String errorMessage) {
        try {
            String safeReport = report == null ? "" : report;
            String preview = safeReport.length() > 200
                    ? safeReport.substring(0, 200) + "..."
                    : safeReport;

            DeliveryLog log2 = DeliveryLog.builder()
                    .reportWeek(reportWeek)
                    .deliveredAt(LocalDateTime.now())
                    .channel("slack")
                    .status(status)
                    .errorMessage(errorMessage)
                    .reportPreview(preview)
                    .build();

            return deliveryLogRepository.save(log2);

        } catch (Exception e) {
            log.error("Failed to save delivery log", e);
            return null;
        }
    }
}