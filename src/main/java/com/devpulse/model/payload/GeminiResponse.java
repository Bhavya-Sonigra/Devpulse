package com.devpulse.model.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiResponse {

    @JsonProperty("candidates")
    private List<Candidate> candidates;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {

        @JsonProperty("content")
        private Content content;

        @JsonProperty("finishReason")
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {

        @JsonProperty("parts")
        private List<Part> parts;

        @JsonProperty("role")
        private String role;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {

        @JsonProperty("text")
        private String text;
    }

    public String extractText() {
        try {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Candidate first = candidates.get(0);
            if (first.getContent() == null) return null;
            if (first.getContent().getParts() == null
                    || first.getContent().getParts().isEmpty()) {
                return null;
            }
            return first.getContent().getParts().get(0).getText();
        } catch (Exception e) {
            return null;
        }
    }
}