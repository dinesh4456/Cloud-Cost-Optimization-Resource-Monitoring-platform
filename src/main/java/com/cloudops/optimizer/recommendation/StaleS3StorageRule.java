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
 * Rule 4: STALE_S3_DATA
 * Detects S3 buckets with storage exceeding threshold (e.g. 50 GB) and data older than 90 days in Standard tier.
 * Recommends setting S3 Lifecycle Rules to transition cold data to Glacier / Glacier Instant Retrieval.
 */
@Component
public class StaleS3StorageRule implements OptimizationRule {

    public static final String RULE_CODE = "STALE_S3_DATA";

    private final OptimizerProperties properties;
    private final AwsPricingCatalog pricingCatalog;

    public StaleS3StorageRule(OptimizerProperties properties, AwsPricingCatalog pricingCatalog) {
        this.properties = properties;
        this.pricingCatalog = pricingCatalog;
    }

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<Recommendation> evaluate(ResourceSnapshot snapshot, ScanJob scanJob) {
        if (snapshot.getResourceType() != ResourceType.S3) {
            return Optional.empty();
        }

        double sizeGb = snapshot.attributeAsDouble("estimatedSizeGb", 0.0);
        double thresholdGb = properties.getRules().getS3StorageThresholdGb();
        double ageDays = snapshot.attributeAsDouble("oldestObjectAgeDays", 0.0);
        int ageThresholdDays = properties.getRules().getS3GlacierAgeDays();
        String storageClass = snapshot.attribute("storageClass");

        if (sizeGb >= thresholdGb && ageDays >= ageThresholdDays && "STANDARD".equalsIgnoreCase(storageClass)) {
            BigDecimal potentialSavings = pricingCatalog.getS3GlacierTransitionSavings(sizeGb);

            String title = String.format("S3 Cold Storage Optimization: %s (%.0f GB)", snapshot.getResourceName(), sizeGb);
            String description = String.format(
                    "Bucket '%s' contains approximately %.1f GB of data with objects older than %d days retained in S3 Standard storage. Enabling an S3 Lifecycle Rule to transition older objects to S3 Glacier Instant Retrieval can reduce storage costs by up to 82%%.",
                    snapshot.getResourceName(),
                    sizeGb,
                    (int) ageDays
            );

            return Optional.of(new Recommendation(
                    scanJob,
                    snapshot,
                    RULE_CODE,
                    title,
                    description,
                    Severity.MEDIUM,
                    potentialSavings
            ));
        }

        return Optional.empty();
    }
}
