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
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Content {

        @JsonProperty("parts")
        private List<Part> parts;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Part {

        @JsonProperty("text")
        private String text;
    }

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.get(0).getContent() == null) {
            return null;
        }
        if (candidates.get(0).getContent().getParts() == null ||
                candidates.get(0).getContent().getParts().isEmpty()) {
            return null;
        }
        return candidates.get(0).getContent().getParts().get(0).getText();
    }
}