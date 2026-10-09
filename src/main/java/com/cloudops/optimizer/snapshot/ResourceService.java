package com.cloudops.optimizer.snapshot;

import com.cloudops.optimizer.common.ResourceNotFoundException;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.scan.ScanService;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {

    private final ResourceSnapshotRepository snapshotRepository;
    private final ResourceMetricRepository metricRepository;
    private final ScanService scanService;

    public ResourceService(
            ResourceSnapshotRepository snapshotRepository,
            ResourceMetricRepository metricRepository,
            ScanService scanService) {
        this.snapshotRepository = snapshotRepository;
        this.metricRepository = metricRepository;
        this.scanService = scanService;
    }

    public List<ResourceSnapshot> findLatestResources(ResourceType type) {
        ScanJob latest = scanService.findLatestSuccessfulScan();
        if (latest == null) {
            return Collections.emptyList();
        }
        if (type != null) {
            return snapshotRepository.findByScanJobAndResourceType(latest, type);
        }
        return snapshotRepository.findByScanJob(latest);
    }

    public List<ResourceSnapshot> findByScanJob(ScanJob scanJob, ResourceType type) {
        if (type != null) {
            return snapshotRepository.findByScanJobAndResourceType(scanJob, type);
        }
        return snapshotRepository.findByScanJob(scanJob);
    }

    public ResourceSnapshot findById(Long id) {
        return snapshotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource snapshot not found: " + id));
    }

    public List<ResourceMetric> findMetricsBySnapshotId(Long snapshotId) {
        return metricRepository.findBySnapshotIdOrderByPeriodStartAsc(snapshotId);
    }
}
