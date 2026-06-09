package com.devpulse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class AppConfig {

    @Value("${devpulse.github.webhook-secret}")
    private String githubWebhookSecret;

    @Value("${devpulse.gemini.api-key}")
    private String geminiApiKey;

    @Value("${devpulse.gemini.api-url}")
    private String geminiApiUrl;

    @Value("${devpulse.slack.webhook-url:}")
    private String slackWebhookUrl;
}