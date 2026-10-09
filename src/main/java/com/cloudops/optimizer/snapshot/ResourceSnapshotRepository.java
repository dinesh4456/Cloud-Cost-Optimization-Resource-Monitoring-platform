package com.cloudops.optimizer.snapshot;

import com.cloudops.optimizer.scan.ScanJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceSnapshotRepository extends JpaRepository<ResourceSnapshot, Long> {

    List<ResourceSnapshot> findByScanJob(ScanJob scanJob);

    List<ResourceSnapshot> findByScanJobAndResourceType(ScanJob scanJob, ResourceType resourceType);
}
