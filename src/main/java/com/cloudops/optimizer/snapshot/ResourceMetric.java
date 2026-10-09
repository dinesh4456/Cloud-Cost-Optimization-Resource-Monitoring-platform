package com.cloudops.optimizer.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "resource_metrics")
public class ResourceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private ResourceSnapshot snapshot;

    @Column(name = "metric_name", nullable = false, length = 80)
    private String metricName;

    @Column(name = "metric_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal metricValue;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    protected ResourceMetric() {
    }

    public ResourceMetric(
            String metricName, BigDecimal metricValue, String unit, Instant periodStart, Instant periodEnd) {
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.unit = unit;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public void setSnapshot(ResourceSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Long getId() {
        return id;
    }

    public ResourceSnapshot getSnapshot() {
        return snapshot;
    }

    public String getMetricName() {
        return metricName;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }
}
