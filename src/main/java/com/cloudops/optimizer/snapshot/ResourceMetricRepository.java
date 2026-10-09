package com.cloudops.optimizer.snapshot;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceMetricRepository extends JpaRepository<ResourceMetric, Long> {

    List<ResourceMetric> findBySnapshotIdOrderByPeriodStartAsc(Long snapshotId);
}
