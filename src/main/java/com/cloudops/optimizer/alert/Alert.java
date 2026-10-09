package com.cloudops.optimizer.alert;

import com.cloudops.optimizer.recommendation.Recommendation;
import com.cloudops.optimizer.recommendation.Severity;
import com.cloudops.optimizer.scan.ScanJob;
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
import java.time.Instant;

@Entity
@Table(name = "alerts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_job_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "snapshots", "recommendations"})
    private ScanJob scanJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "scanJob", "snapshot"})
    private Recommendation recommendation;

    @Column(name = "alert_type", nullable = false, length = 40)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Alert() {
    }

    public Alert(ScanJob scanJob, Recommendation recommendation, String alertType, Severity severity, String message) {
        this.scanJob = scanJob;
        this.recommendation = recommendation;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public ScanJob getScanJob() {
        return scanJob;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public String getAlertType() {
        return alertType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
