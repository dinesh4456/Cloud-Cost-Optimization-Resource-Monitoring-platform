package com.cloudops.optimizer.report;

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
import java.time.Instant;

@Entity
@Table(name = "reports")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "generated_by_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "roles", "hibernateLazyInitializer", "handler"})
    private User generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "summary_json", columnDefinition = "TEXT")
    private String summaryJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Report() {
    }

    public Report(
            User generatedBy,
            ReportType reportType,
            Instant periodStart,
            Instant periodEnd,
            String filePath,
            String summaryJson) {
        this.generatedBy = generatedBy;
        this.reportType = reportType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.filePath = filePath;
        this.summaryJson = summaryJson;
    }

    public Long getId() {
        return id;
    }

    public User getGeneratedBy() {
        return generatedBy;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
