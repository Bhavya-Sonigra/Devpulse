package com.devpulse.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "total_commits", nullable = false)
    private int totalCommits;

    @Column(name = "prs_opened", nullable = false)
    private int prsOpened;

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

    @Column(name = "commits_by_user", columnDefinition = "text")
    private String commitsByUser;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}