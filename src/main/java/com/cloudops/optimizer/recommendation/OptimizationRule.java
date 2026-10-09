package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import java.util.Optional;

public interface OptimizationRule {

    String getRuleCode();

    Optional<Recommendation> evaluate(ResourceSnapshot snapshot, ScanJob scanJob);
}
