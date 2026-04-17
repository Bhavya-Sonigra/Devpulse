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
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlackDeliveryService {

    private final AppConfig appConfig;
    private final RestTemplate restTemplate;
    private final DeliveryLogRepository deliveryLogRepository;

    public void deliver(String report, LocalDate reportWeek) {
        log.info("Delivering report to Slack for week: {}", reportWeek);

        String status;
        String errorMessage = null;

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
                    appConfig.getSlackWebhookUrl(),
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

        saveDeliveryLog(report, reportWeek, status, errorMessage);
    }

    private void saveDeliveryLog(String report,
                                 LocalDate reportWeek,
                                 String status,
                                 String errorMessage) {
        try {
            String preview = report.length() > 200
                    ? report.substring(0, 200) + "..."
                    : report;

            DeliveryLog log2 = DeliveryLog.builder()
                    .reportWeek(reportWeek)
                    .deliveredAt(LocalDateTime.now())
                    .channel(appConfig.getSlackWebhookUrl())
                    .status(status)
                    .errorMessage(errorMessage)
                    .reportPreview(preview)
                    .build();

            deliveryLogRepository.save(log2);

        } catch (Exception e) {
            log.error("Failed to save delivery log", e);
        }
    }
}