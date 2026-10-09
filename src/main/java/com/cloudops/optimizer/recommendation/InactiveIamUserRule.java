package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Rule 5: INACTIVE_IAM_USER
 * Detects IAM users with stale active access keys older than 90 days or dormant users with no recent login.
 */
@Component
public class InactiveIamUserRule implements OptimizationRule {

    public static final String RULE_CODE = "INACTIVE_IAM_USER";

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<Recommendation> evaluate(ResourceSnapshot snapshot, ScanJob scanJob) {
        if (snapshot.getResourceType() != ResourceType.IAM) {
            return Optional.empty();
        }

        double oldestKeyAge = snapshot.attributeAsDouble("oldestKeyAgeDays", 0.0);
        String passwordLastUsed = snapshot.attribute("passwordLastUsed");
        int activeKeys = (int) snapshot.attributeAsDouble("activeAccessKeys", 0.0);

        if (oldestKeyAge >= 90 || (activeKeys > 0 && "NEVER".equalsIgnoreCase(passwordLastUsed))) {
            String title = String.format("Stale IAM Access Credentials: %s", snapshot.getResourceName());
            String description = String.format(
                    "IAM User '%s' has active access keys that are %d days old without recent credential rotation. Follow AWS security best practices by rotating or deactivating stale credentials to prevent unauthorized resource footprint expansion.",
                    snapshot.getResourceName(),
                    (int) oldestKeyAge
            );

            return Optional.of(new Recommendation(
                    scanJob,
                    snapshot,
                    RULE_CODE,
                    title,
                    description,
                    Severity.LOW,
                    BigDecimal.ZERO
            ));
        }

        return Optional.empty();
    }
}
