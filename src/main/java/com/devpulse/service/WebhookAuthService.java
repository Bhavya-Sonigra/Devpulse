package com.devpulse.service;

import com.devpulse.config.AppConfig;
import com.devpulse.exception.InvalidSignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookAuthService {

    private final AppConfig appConfig;

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    public void validateSignature(String payload, String signatureHeader) {
        log.debug("Validating webhook signature");

        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new InvalidSignatureException("Missing signature header");
        }

        // 1. Defend against Windows/WSL hidden \r characters in the secret
        String secret = appConfig.getGithubWebhookSecret().trim();

        // 2. NUCLEAR DEBUGGING LOGS
        log.info("--- WEBHOOK DEBUG ---");
        log.info("Secret Length: {}", secret.length());
        log.info("Payload Length: {}", payload.length());

        String receivedHash = signatureHeader.replace(SIGNATURE_PREFIX, "");
        String computedHash = computeHmacSha256(payload, secret);

        log.info("GitHub Hash   : {}", receivedHash);
        log.info("Computed Hash : {}", computedHash);
        log.info("---------------------");

        if (!constantTimeEquals(receivedHash, computedHash)) {
            log.warn("Webhook signature mismatch - possible spoofing attempt");
            throw new InvalidSignatureException("Signature verification failed");
        }

        log.info("Webhook signature validated successfully");
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256", e);
            throw new RuntimeException("Signature computation failed", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}