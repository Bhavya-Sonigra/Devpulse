package com.devpulse.model.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PullRequestEventPayload {

    @JsonProperty("action")
    private String action;

    @JsonProperty("number")
    private int number;

    @JsonProperty("repository")
    private PushEventPayload.Repository repository;

    @JsonProperty("pull_request")
    private PullRequest pullRequest;

    @JsonProperty("sender")
    private Sender sender;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {

        @JsonProperty("id")
        private long id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("state")
        private String state;

        @JsonProperty("merged")
        private boolean merged;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        @JsonProperty("closed_at")
        private String closedAt;

        @JsonProperty("merged_at")
        private String mergedAt;

        @JsonProperty("head")
        private Branch head;

        @JsonProperty("base")
        private Branch base;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Branch {

        @JsonProperty("ref")
        private String ref;

        @JsonProperty("label")
        private String label;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sender {

        @JsonProperty("login")
        private String login;
    }
}