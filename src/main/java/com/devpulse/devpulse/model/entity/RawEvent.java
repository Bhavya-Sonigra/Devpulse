package com.devpulse.devpulse.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "raw_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "repo_name", nullable = false, length = 255)
    private String repoName;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "branch", length = 255)
    private String branch;

    @Column(name = "delivery_id", unique = true, length = 100)
    private String deliveryId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed", nullable = false)
    private boolean processed;
}