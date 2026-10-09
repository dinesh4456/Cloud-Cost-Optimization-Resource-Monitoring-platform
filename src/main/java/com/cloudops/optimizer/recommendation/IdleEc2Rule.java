package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.aws.client.AwsPricingCatalog;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Rule 1: IDLE_EC2
 * Detects running EC2 instances with 7-day average CPU utilization < threshold (default 10%).
 */
@Component
public class IdleEc2Rule implements OptimizationRule {

    public static final String RULE_CODE = "IDLE_EC2";

    private final OptimizerProperties properties;
    private final AwsPricingCatalog pricingCatalog;

    public IdleEc2Rule(OptimizerProperties properties, AwsPricingCatalog pricingCatalog) {
        this.properties = properties;
        this.pricingCatalog = pricingCatalog;
    }

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<Recommendation> evaluate(ResourceSnapshot snapshot, ScanJob scanJob) {
        if (snapshot.getResourceType() != ResourceType.EC2) {
            return Optional.empty();
        }

        if (!"running".equalsIgnoreCase(snapshot.getState())) {
            return Optional.empty();
        }

        BigDecimal cpuAvg = snapshot.getCpuAvg7d();
        if (cpuAvg == null) {
            return Optional.empty();
        }

        double threshold = properties.getRules().getIdleCpuPercent();
        if (cpuAvg.doubleValue() < threshold) {
            String instanceType = snapshot.attribute("instanceType");
            BigDecimal monthlyCost = pricingCatalog.getEc2MonthlyCost(instanceType);

            String title = String.format("Idle EC2 Instance: %s (%s)", snapshot.getResourceName(), instanceType);
            String description = String.format(
                    "Instance %s (%s) has an average 7-day CPU utilization of %.2f%%, which is below the %.1f%% idle threshold. Consider downsizing or stopping this instance to eliminate waste.",
                    snapshot.getResourceId(),
                    instanceType != null ? instanceType : "unknown",
                    cpuAvg.doubleValue(),
                    threshold
            );

            return Optional.of(new Recommendation(
                    scanJob,
                    snapshot,
                    RULE_CODE,
                    title,
                    description,
                    Severity.HIGH,
                    monthlyCost
            ));
        }

        return Optional.empty();
    }
}
