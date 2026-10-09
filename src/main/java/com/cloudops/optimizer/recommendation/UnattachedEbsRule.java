package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.aws.client.AwsPricingCatalog;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Rule 2: UNATTACHED_EBS
 * Detects EBS volumes in 'available' state that are not attached to any EC2 instance.
 */
@Component
public class UnattachedEbsRule implements OptimizationRule {

    public static final String RULE_CODE = "UNATTACHED_EBS";

    private final AwsPricingCatalog pricingCatalog;

    public UnattachedEbsRule(AwsPricingCatalog pricingCatalog) {
        this.pricingCatalog = pricingCatalog;
    }

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<Recommendation> evaluate(ResourceSnapshot snapshot, ScanJob scanJob) {
        if (snapshot.getResourceType() != ResourceType.EBS) {
            return Optional.empty();
        }

        boolean isAvailable = "available".equalsIgnoreCase(snapshot.getState());
        boolean notAttached = "false".equalsIgnoreCase(snapshot.attribute("attached"));

        if (isAvailable || notAttached) {
            double sizeGb = snapshot.attributeAsDouble("sizeGb", 0.0);
            String volType = snapshot.attribute("volumeType");
            BigDecimal monthlyCost = pricingCatalog.getEbsMonthlyCost(sizeGb, volType);

            String title = String.format("Unattached EBS Volume: %s (%.0f GB)", snapshot.getResourceName(), sizeGb);
            String description = String.format(
                    "EBS Volume %s (%.0f GB, %s) is in 'available' state and not attached to any running instance. It incurs continuous block storage charges without active usage. Snapshot and delete if not needed.",
                    snapshot.getResourceId(),
                    sizeGb,
                    volType != null ? volType : "gp3"
            );

            return Optional.of(new Recommendation(
                    scanJob,
                    snapshot,
                    RULE_CODE,
                    title,
                    description,
                    Severity.MEDIUM,
                    monthlyCost
            ));
        }

        return Optional.empty();
    }
}
