package com.devpulse.controller;

import com.devpulse.model.entity.User;
import com.devpulse.repository.UserRepository;
import com.devpulse.security.DevPulseUserDetails;
import com.devpulse.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

    private final GitHubService gitHubService;
    private final UserRepository userRepository;

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableRepos(@AuthenticationPrincipal DevPulseUserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(gitHubService.getAvailableRepos(user.getGithubAccessToken()));
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectRepo(
            @AuthenticationPrincipal DevPulseUserDetails userDetails,
            @RequestBody Map<String, String> payload) {

        String repoFullName = payload.get("repoFullName");
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        gitHubService.registerWebhook(repoFullName, user.getGithubAccessToken(), userDetails.getTeamId());

        return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook connected"));
    }
}