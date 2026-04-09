package com.devpulse.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;


@Entity
@Table(name = "weekly_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMetrics {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "week_start", nullable = false, unique = true)
    private LocalDate weekStart;

    @Column(name = "prs_merged", nullable = false)
    private int prsMerged;

    @Column(name = "prs_still_open", nullable = false)
    private int prsStillOpen;

    @Column(name = "avg_pr_open_hours")
    private Double avgPrOpenHours;

    @Column(name = "top_contributor", length = 100)
    private String topContributor;

    @Column(name = "bug_fix_count", nullable = false)
    private int bugFixCount;

    @Column(name = "feature_count", nullable = false)
    private int featureCount;

    @Column(name = "most_changed_file", length = 255)
    private String mostChangedFile;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "commits_by_user", columnDefinition = "jsonb")
    private String commitsByUser;

    @Column(name = "computed_at", nullable = false)
    private java.time.LocalDateTime computedAt;
}
