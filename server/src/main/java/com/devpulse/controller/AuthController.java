package com.devpulse.controller;

import com.devpulse.model.entity.Team;
import com.devpulse.model.entity.User;
import com.devpulse.repository.TeamRepository;
import com.devpulse.repository.UserRepository;
import com.devpulse.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final JwtService jwtService;

    @PostMapping("/github")
    public ResponseEntity<?> githubLogin(@RequestBody Map<String, Object> body) {
        Long githubId = Long.valueOf(body.get("githubId").toString());
        String username = body.get("login").toString();
        String accessToken = body.get("accessToken").toString();
        String email = body.get("email") != null
                ? body.get("email").toString() : null;

        Optional<User> existing = userRepository.findByGithubId(githubId);

        User user;
        if (existing.isPresent()) {
            user = existing.get();
            user.setGithubAccessToken(accessToken);
            user = userRepository.save(user);
        } else {
            Team team = Team.builder()
                    .name(username + "'s team")
                    .plan("free")
                    .build();
            team = teamRepository.save(team);

            user = User.builder()
                    .teamId(team.getId())
                    .githubId(githubId)
                    .githubUsername(username)
                    .githubAccessToken(accessToken)
                    .email(email)
                    .build();
            user = userRepository.save(user);
        }

        String jwt = jwtService.generateToken(
                user.getId(), user.getTeamId(), user.getGithubUsername());

        return ResponseEntity.ok(Map.of(
                "token", jwt,
                "username", user.getGithubUsername(),
                "teamId", user.getTeamId()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestAttribute("teamId") String teamId) {
        return ResponseEntity.ok(Map.of("teamId", teamId));
    }
}