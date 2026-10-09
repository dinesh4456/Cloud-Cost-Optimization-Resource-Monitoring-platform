package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.scan.ScanJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByScanJobOrderByEstimatedMonthlySavingsDesc(ScanJob scanJob);

    List<Recommendation> findAllByOrderByCreatedAtDesc();
}
