package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.aws.client.AwsPricingCatalog;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Rule 3: UNUSED_EIP
 * Detects Elastic IP addresses allocated but not associated with an EC2 instance or ENI.
 */
@Component
public class UnusedEipRule implements OptimizationRule {

    public static final String RULE_CODE = "UNUSED_EIP";

    private final AwsPricingCatalog pricingCatalog;

    public UnusedEipRule(AwsPricingCatalog pricingCatalog) {
        this.pricingCatalog = pricingCatalog;
    }

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<Recommendation> evaluate(ResourceSnapshot snapshot, ScanJob scanJob) {
        if (snapshot.getResourceType() != ResourceType.EIP) {
            return Optional.empty();
        }

        boolean isUnassociated = "unassociated".equalsIgnoreCase(snapshot.getState());
        boolean notAssociated = "false".equalsIgnoreCase(snapshot.attribute("associated"));

        if (isUnassociated || notAssociated) {
            String publicIp = snapshot.attribute("publicIp");
            if (publicIp == null || publicIp.isBlank()) {
                publicIp = snapshot.getResourceId();
            }
            BigDecimal monthlyCost = pricingCatalog.getUnusedEipMonthlyCost();

            String title = String.format("Unused Elastic IP: %s", publicIp);
            String description = String.format(
                    "Elastic IP %s is allocated to your AWS account but not attached to any running EC2 instance or Network Interface. AWS charges for unassociated static IPs ($0.005/hr). Release this IP to stop charges.",
                    publicIp
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
