package com.devpulse.model.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PushEventPayload {

    @JsonProperty("ref")
    private String ref;

    @JsonProperty("repository")
    private Repository repository;

    @JsonProperty("pusher")
    private Pusher pusher;

    @JsonProperty("commits")
    private List<Commit> commits;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {

        @JsonProperty("name")
        private String name;

        @JsonProperty("full_name")
        private String fullName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pusher {

        @JsonProperty("name")
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commit {

        @JsonProperty("id")
        private String id;

        @JsonProperty("message")
        private String message;

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("added")
        private List<String> added;

        @JsonProperty("modified")
        private List<String> modified;

        @JsonProperty("removed")
        private List<String> removed;
    }
}