package com.devpulse.controller;

import com.devpulse.exception.InvalidSignatureException;
import com.devpulse.service.WebhookAuthService;
import com.devpulse.service.WebhookProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/webhooks")
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookAuthService webhookAuthService;
    private final WebhookProcessorService webhookProcessorService;

    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader("X-GitHub-Delivery") String deliveryId,
            @RequestBody byte[] rawPayloadBytes) { // <-- Capture exact bytes

        log.info("Received GitHub webhook - event: {}, delivery: {}",
                eventType, deliveryId);

        try {
            // Convert to String using UTF-8 immediately
            String rawPayload = new String(rawPayloadBytes, StandardCharsets.UTF_8);

            webhookAuthService.validateSignature(rawPayload, signature);
            webhookProcessorService.processWebhook(eventType,
                    deliveryId,
                    rawPayload);

            return ResponseEntity
                    .ok("Webhook processed successfully");

        } catch (InvalidSignatureException e) {
            log.warn("Rejected webhook - invalid signature: {}",
                    e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid signature");
        } catch (Exception e) {
            log.error("Unexpected error processing webhook", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payload");
        }
    }
}