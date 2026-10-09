package com.cloudops.optimizer.scan;

import com.cloudops.optimizer.user.User;
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
@Table(name = "scan_jobs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ScanJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "triggered_by_id")
    @JsonIgnoreProperties({"passwordHash", "roles", "hibernateLazyInitializer", "handler"})
    private User triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanJobStatus status;

    @Column(nullable = false, length = 40)
    private String region;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "resources_scanned_count", nullable = false)
    private int resourcesScannedCount;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "estimated_monthly_cost", precision = 12, scale = 2)
    private BigDecimal estimatedMonthlyCost;

    @Column(name = "estimated_monthly_savings", precision = 12, scale = 2)
    private BigDecimal estimatedMonthlySavings;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ScanJob() {
    }

    public ScanJob(User triggeredBy, String region) {
        this.triggeredBy = triggeredBy;
        this.region = region;
        this.status = ScanJobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getTriggeredBy() {
        return triggeredBy;
    }

    public ScanJobStatus getStatus() {
        return status;
    }

    public void setStatus(ScanJobStatus status) {
        this.status = status;
    }

    public String getRegion() {
        return region;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getResourcesScannedCount() {
        return resourcesScannedCount;
    }

    public void setResourcesScannedCount(int resourcesScannedCount) {
        this.resourcesScannedCount = resourcesScannedCount;
    }

    public Integer getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Integer healthScore) {
        this.healthScore = healthScore;
    }

    public BigDecimal getEstimatedMonthlyCost() {
        return estimatedMonthlyCost;
    }

    public void setEstimatedMonthlyCost(BigDecimal estimatedMonthlyCost) {
        this.estimatedMonthlyCost = estimatedMonthlyCost;
    }

    public BigDecimal getEstimatedMonthlySavings() {
        return estimatedMonthlySavings;
    }

    public void setEstimatedMonthlySavings(BigDecimal estimatedMonthlySavings) {
        this.estimatedMonthlySavings = estimatedMonthlySavings;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
