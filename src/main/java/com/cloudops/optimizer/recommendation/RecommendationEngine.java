package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecommendationEngine {

    private static final Logger log = LoggerFactory.getLogger(RecommendationEngine.class);

    private final List<OptimizationRule> rules;

    public RecommendationEngine(List<OptimizationRule> rules) {
        this.rules = rules;
    }

    public List<Recommendation> evaluate(ScanJob scanJob, List<ResourceSnapshot> snapshots) {
        List<Recommendation> recommendations = new ArrayList<>();

        for (ResourceSnapshot snapshot : snapshots) {
            for (OptimizationRule rule : rules) {
                try {
                    Optional<Recommendation> result = rule.evaluate(snapshot, scanJob);
                    result.ifPresent(recommendations::add);
                } catch (Exception ex) {
                    log.error("Rule {} evaluation failed for resource {}: {}",
                            rule.getRuleCode(), snapshot.getResourceId(), ex.getMessage());
                }
            }
        }

        log.info("RecommendationEngine evaluated {} rules on {} snapshots -> generated {} findings",
                rules.size(), snapshots.size(), recommendations.size());
        return recommendations;
    }
}
