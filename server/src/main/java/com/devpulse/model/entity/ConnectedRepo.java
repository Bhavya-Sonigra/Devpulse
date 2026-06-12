package com.devpulse.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "connected_repos",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"team_id", "repo_full_name"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConnectedRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    @Column(name = "webhook_id", nullable = false)
    private Long webhookId;

    @Column(name = "webhook_secret", nullable = false)
    private String webhookSecret;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}