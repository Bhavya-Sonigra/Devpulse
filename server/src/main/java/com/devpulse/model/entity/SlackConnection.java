package com.devpulse.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "slack_connections")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlackConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "team_id", nullable = false, unique = true)
    private UUID teamId;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(name = "channel_name", nullable = false)
    private String channelName;

    @Column(name = "bot_token", nullable = false, columnDefinition = "TEXT")
    private String botToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}