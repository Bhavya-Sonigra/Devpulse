package com.devpulse.service;

import com.devpulse.config.AppConfig;
import com.devpulse.exception.InvalidSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookAuthServiceTest {

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private WebhookAuthService webhookAuthService;

    private static final String SECRET = "test-secret";
    private static final String PAYLOAD = "{\"ref\":\"refs/heads/main\"}";

    @BeforeEach
    void setUp() {
        when(appConfig.getGithubWebhookSecret()).thenReturn(SECRET);
    }

    @Test
    void validSignature_shouldNotThrow() {
        String validSignature = computeSignature(PAYLOAD, SECRET);
        assertDoesNotThrow(() ->
                webhookAuthService.validateSignature(PAYLOAD, validSignature));
    }

    @Test
    void invalidSignature_shouldThrowInvalidSignatureException() {
        assertThrows(InvalidSignatureException.class, () ->
                webhookAuthService.validateSignature(
                        PAYLOAD, "sha256=invalidsignature"));
    }

    @Test
    void missingSignatureHeader_shouldThrow() {
        assertThrows(InvalidSignatureException.class, () ->
                webhookAuthService.validateSignature(PAYLOAD, null));
    }

    @Test
    void blankSignatureHeader_shouldThrow() {
        assertThrows(InvalidSignatureException.class, () ->
                webhookAuthService.validateSignature(PAYLOAD, "  "));
    }

    @Test
    void wrongFormatSignature_shouldThrow() {
        assertThrows(InvalidSignatureException.class, () ->
                webhookAuthService.validateSignature(
                        PAYLOAD, "md5=somehash"));
    }

    private String computeSignature(String payload, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac
                    .getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec =
                    new javax.crypto.spec.SecretKeySpec(
                            secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(
                    payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return "sha256=" + hex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}