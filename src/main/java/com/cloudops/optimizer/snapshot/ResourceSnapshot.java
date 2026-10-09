package com.cloudops.optimizer.snapshot;

import com.cloudops.optimizer.common.JsonMapConverter;
import com.cloudops.optimizer.scan.ScanJob;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "resource_snapshots")
public class ResourceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_job_id", nullable = false)
    private ScanJob scanJob;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false, length = 255)
    private String resourceId;

    @Column(name = "resource_name", length = 255)
    private String resourceName;

    @Column(nullable = false, length = 40)
    private String region;

    @Column(length = 80)
    private String state;

    @Column(name = "cpu_avg_7d", precision = 8, scale = 2)
    private BigDecimal cpuAvg7d;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResourceMetric> metrics = new ArrayList<>();

    protected ResourceSnapshot() {
    }

    public ResourceSnapshot(
            ScanJob scanJob,
            ResourceType resourceType,
            String resourceId,
            String resourceName,
            String region,
            String state) {
        this.scanJob = scanJob;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.region = region;
        this.state = state;
    }

    public void addMetric(ResourceMetric metric) {
        metrics.add(metric);
        metric.setSnapshot(this);
    }

    public String attribute(String key) {
        return attributes.get(key);
    }

    public double attributeAsDouble(String key, double fallback) {
        String raw = attributes.get(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public Long getId() {
        return id;
    }

    public ScanJob getScanJob() {
        return scanJob;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getRegion() {
        return region;
    }

    public String getState() {
        return state;
    }

    public BigDecimal getCpuAvg7d() {
        return cpuAvg7d;
    }

    public void setCpuAvg7d(BigDecimal cpuAvg7d) {
        this.cpuAvg7d = cpuAvg7d;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public List<ResourceMetric> getMetrics() {
        return metrics;
    }
}
