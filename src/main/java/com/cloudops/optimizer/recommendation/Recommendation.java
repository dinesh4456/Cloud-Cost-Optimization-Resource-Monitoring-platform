package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "recommendations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_job_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "snapshots", "recommendations"})
    private ScanJob scanJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "metrics"})
    private ResourceSnapshot snapshot;

    @Column(name = "rule_code", nullable = false, length = 40)
    private String ruleCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(name = "estimated_monthly_savings", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedMonthlySavings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status = RecommendationStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Recommendation() {
    }

    public Recommendation(
            ScanJob scanJob,
            ResourceSnapshot snapshot,
            String ruleCode,
            String title,
            String description,
            Severity severity,
            BigDecimal estimatedMonthlySavings) {
        this.scanJob = scanJob;
        this.snapshot = snapshot;
        this.ruleCode = ruleCode;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.estimatedMonthlySavings = estimatedMonthlySavings;
    }

    public Long getId() {
        return id;
    }

    public ScanJob getScanJob() {
        return scanJob;
    }

    public ResourceSnapshot getSnapshot() {
        return snapshot;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    public BigDecimal getEstimatedMonthlySavings() {
        return estimatedMonthlySavings;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
