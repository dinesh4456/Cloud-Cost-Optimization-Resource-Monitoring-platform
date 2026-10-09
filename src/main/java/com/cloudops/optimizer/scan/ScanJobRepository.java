package com.cloudops.optimizer.scan;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanJobRepository extends JpaRepository<ScanJob, Long> {

    Optional<ScanJob> findTopByStatusInOrderByCompletedAtDesc(List<ScanJobStatus> statuses);

    List<ScanJob> findAllByOrderByStartedAtDesc();
}
