package com.cloudops.optimizer;

import com.cloudops.optimizer.aws.client.AwsPricingCatalog;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.recommendation.IdleEc2Rule;
import com.cloudops.optimizer.recommendation.InactiveIamUserRule;
import com.cloudops.optimizer.recommendation.OptimizationRule;
import com.cloudops.optimizer.recommendation.Recommendation;
import com.cloudops.optimizer.recommendation.RecommendationEngine;
import com.cloudops.optimizer.recommendation.StaleS3StorageRule;
import com.cloudops.optimizer.recommendation.UnattachedEbsRule;
import com.cloudops.optimizer.recommendation.UnusedEipRule;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationEngineTest {

    private RecommendationEngine engine;
    private OptimizerProperties properties;
    private AwsPricingCatalog pricingCatalog;

    @BeforeEach
    void setUp() {
        properties = new OptimizerProperties();
        pricingCatalog = new AwsPricingCatalog();

        List<OptimizationRule> rules = List.of(
                new IdleEc2Rule(properties, pricingCatalog),
                new UnattachedEbsRule(pricingCatalog),
                new UnusedEipRule(pricingCatalog),
                new StaleS3StorageRule(properties, pricingCatalog),
                new InactiveIamUserRule()
        );

        engine = new RecommendationEngine(rules);
    }

    @Test
    void testIdleEc2RuleTriggers() {
        ScanJob job = new ScanJob(null, "ap-south-1");
        ResourceSnapshot snapshot = new ResourceSnapshot(job, ResourceType.EC2, "i-test-123", "test-server", "ap-south-1", "running");
        snapshot.setCpuAvg7d(new BigDecimal("4.50"));
        snapshot.setAttributes(Map.of("instanceType", "t3.medium"));

        List<Recommendation> findings = engine.evaluate(job, List.of(snapshot));
        assertEquals(1, findings.size());
        assertEquals("IDLE_EC2", findings.get(0).getRuleCode());
        assertTrue(findings.get(0).getEstimatedMonthlySavings().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testUnattachedEbsRuleTriggers() {
        ScanJob job = new ScanJob(null, "ap-south-1");
        ResourceSnapshot snapshot = new ResourceSnapshot(job, ResourceType.EBS, "vol-test-123", "test-vol", "ap-south-1", "available");
        snapshot.setAttributes(Map.of("sizeGb", "100", "attached", "false", "volumeType", "gp3"));

        List<Recommendation> findings = engine.evaluate(job, List.of(snapshot));
        assertEquals(1, findings.size());
        assertEquals("UNATTACHED_EBS", findings.get(0).getRuleCode());
        assertEquals(new BigDecimal("8.00"), findings.get(0).getEstimatedMonthlySavings());
    }

    @Test
    void testUnusedEipRuleTriggers() {
        ScanJob job = new ScanJob(null, "ap-south-1");
        ResourceSnapshot snapshot = new ResourceSnapshot(job, ResourceType.EIP, "eipalloc-123", "test-ip", "ap-south-1", "unassociated");
        snapshot.setAttributes(Map.of("publicIp", "15.207.1.1", "associated", "false"));

        List<Recommendation> findings = engine.evaluate(job, List.of(snapshot));
        assertEquals(1, findings.size());
        assertEquals("UNUSED_EIP", findings.get(0).getRuleCode());
        assertEquals(new BigDecimal("3.65"), findings.get(0).getEstimatedMonthlySavings());
    }

    @Test
    void testStaleS3StorageRuleTriggers() {
        ScanJob job = new ScanJob(null, "ap-south-1");
        ResourceSnapshot snapshot = new ResourceSnapshot(job, ResourceType.S3, "test-bucket", "test-bucket", "ap-south-1", "ACTIVE");
        snapshot.setAttributes(Map.of(
                "estimatedSizeGb", "100.00",
                "oldestObjectAgeDays", "120",
                "storageClass", "STANDARD"
        ));

        List<Recommendation> findings = engine.evaluate(job, List.of(snapshot));
        assertEquals(1, findings.size());
        assertEquals("STALE_S3_DATA", findings.get(0).getRuleCode());
        assertEquals(new BigDecimal("1.90"), findings.get(0).getEstimatedMonthlySavings());
    }

    @Test
    void testInactiveIamUserRuleTriggers() {
        ScanJob job = new ScanJob(null, "global");
        ResourceSnapshot snapshot = new ResourceSnapshot(job, ResourceType.IAM, "AIDA123", "stale-dev", "global", "ACTIVE");
        snapshot.setAttributes(Map.of(
                "activeAccessKeys", "1",
                "oldestKeyAgeDays", "150",
                "passwordLastUsed", "NEVER"
        ));

        List<Recommendation> findings = engine.evaluate(job, List.of(snapshot));
        assertEquals(1, findings.size());
        assertEquals("INACTIVE_IAM_USER", findings.get(0).getRuleCode());
    }

    @Test
    void testHealthyResourcesProduceNoFindings() {
        ScanJob job = new ScanJob(null, "ap-south-1");
        ResourceSnapshot healthyEc2 = new ResourceSnapshot(job, ResourceType.EC2, "i-healthy", "prod-api", "ap-south-1", "running");
        healthyEc2.setCpuAvg7d(new BigDecimal("45.00"));
        healthyEc2.setAttributes(Map.of("instanceType", "t3.medium"));

        ResourceSnapshot attachedEbs = new ResourceSnapshot(job, ResourceType.EBS, "vol-attached", "root", "ap-south-1", "in-use");
        attachedEbs.setAttributes(Map.of("sizeGb", "50", "attached", "true"));

        List<Recommendation> findings = engine.evaluate(job, List.of(healthyEc2, attachedEbs));
        assertTrue(findings.isEmpty());
    }
}
