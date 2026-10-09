package com.cloudops.optimizer.aws.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * In-memory AWS reference pricing catalog for v1 cost estimation and savings calculation.
 */
@Component
public class AwsPricingCatalog {

    // Monthly baseline cost for common EC2 instance types in USD (730 hours/month)
    private static final Map<String, BigDecimal> EC2_HOURLY_PRICING = Map.ofEntries(
            Map.entry("t2.nano", new BigDecimal("0.0058")),
            Map.entry("t2.micro", new BigDecimal("0.0116")),
            Map.entry("t2.small", new BigDecimal("0.023")),
            Map.entry("t2.medium", new BigDecimal("0.0464")),
            Map.entry("t3.nano", new BigDecimal("0.0052")),
            Map.entry("t3.micro", new BigDecimal("0.0104")),
            Map.entry("t3.small", new BigDecimal("0.0208")),
            Map.entry("t3.medium", new BigDecimal("0.0416")),
            Map.entry("t3.large", new BigDecimal("0.0832")),
            Map.entry("t3.xlarge", new BigDecimal("0.1664")),
            Map.entry("m5.large", new BigDecimal("0.096")),
            Map.entry("m5.xlarge", new BigDecimal("0.192")),
            Map.entry("c5.large", new BigDecimal("0.085")),
            Map.entry("c5.xlarge", new BigDecimal("0.170")),
            Map.entry("r5.large", new BigDecimal("0.126"))
    );

    private static final BigDecimal DEFAULT_EC2_HOURLY = new BigDecimal("0.0416"); // ~t3.medium
    private static final BigDecimal HOURS_PER_MONTH = new BigDecimal("730");
    private static final BigDecimal EBS_GP3_PER_GB_MONTH = new BigDecimal("0.08");
    private static final BigDecimal UNUSED_EIP_MONTHLY = new BigDecimal("3.65");
    private static final BigDecimal S3_STANDARD_PER_GB_MONTH = new BigDecimal("0.023");
    private static final BigDecimal S3_GLACIER_PER_GB_MONTH = new BigDecimal("0.004");

    public BigDecimal getEc2MonthlyCost(String instanceType) {
        if (instanceType == null || instanceType.isBlank()) {
            return DEFAULT_EC2_HOURLY.multiply(HOURS_PER_MONTH).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal hourly = EC2_HOURLY_PRICING.getOrDefault(instanceType.toLowerCase(), DEFAULT_EC2_HOURLY);
        return hourly.multiply(HOURS_PER_MONTH).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getEbsMonthlyCost(double sizeGb, String volumeType) {
        BigDecimal size = BigDecimal.valueOf(Math.max(0, sizeGb));
        return size.multiply(EBS_GP3_PER_GB_MONTH).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getUnusedEipMonthlyCost() {
        return UNUSED_EIP_MONTHLY;
    }

    public BigDecimal getS3MonthlyCost(double sizeGb) {
        BigDecimal size = BigDecimal.valueOf(Math.max(0, sizeGb));
        return size.multiply(S3_STANDARD_PER_GB_MONTH).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getS3GlacierTransitionSavings(double sizeGb) {
        BigDecimal size = BigDecimal.valueOf(Math.max(0, sizeGb));
        BigDecimal diff = S3_STANDARD_PER_GB_MONTH.subtract(S3_GLACIER_PER_GB_MONTH);
        return size.multiply(diff).setScale(2, RoundingMode.HALF_UP);
    }
}
