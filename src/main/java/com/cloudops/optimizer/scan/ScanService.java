package com.cloudops.optimizer.scan;

import com.cloudops.optimizer.activity.ActivityLogService;
import com.cloudops.optimizer.alert.AlertService;
import com.cloudops.optimizer.aws.client.AwsPricingCatalog;
import com.cloudops.optimizer.aws.scanner.ResourceScanner;
import com.cloudops.optimizer.common.CurrentUserService;
import com.cloudops.optimizer.common.ResourceNotFoundException;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.recommendation.Recommendation;
import com.cloudops.optimizer.recommendation.RecommendationEngine;
import com.cloudops.optimizer.recommendation.RecommendationRepository;
import com.cloudops.optimizer.recommendation.Severity;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceSnapshotRepository;
import com.cloudops.optimizer.snapshot.ResourceType;
import com.cloudops.optimizer.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final ScanJobRepository scanJobRepository;
    private final ResourceSnapshotRepository snapshotRepository;
    private final RecommendationRepository recommendationRepository;
    private final List<ResourceScanner> scanners;
    private final RecommendationEngine recommendationEngine;
    private final AlertService alertService;
    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;
    private final AwsPricingCatalog pricingCatalog;
    private final OptimizerProperties properties;

    public ScanService(
            ScanJobRepository scanJobRepository,
            ResourceSnapshotRepository snapshotRepository,
            RecommendationRepository recommendationRepository,
            List<ResourceScanner> scanners,
            RecommendationEngine recommendationEngine,
            AlertService alertService,
            ActivityLogService activityLogService,
            CurrentUserService currentUserService,
            AwsPricingCatalog pricingCatalog,
            OptimizerProperties properties) {
        this.scanJobRepository = scanJobRepository;
        this.snapshotRepository = snapshotRepository;
        this.recommendationRepository = recommendationRepository;
        this.scanners = scanners;
        this.recommendationEngine = recommendationEngine;
        this.alertService = alertService;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;
        this.pricingCatalog = pricingCatalog;
        this.properties = properties;
    }

    public List<ScanJob> findAll() {
        return scanJobRepository.findAllByOrderByStartedAtDesc();
    }

    public ScanJob findById(Long id) {
        return scanJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scan job not found: " + id));
    }

    public ScanJob findLatestSuccessfulScan() {
        return scanJobRepository.findTopByStatusInOrderByCompletedAtDesc(
                List.of(ScanJobStatus.SUCCESS, ScanJobStatus.PARTIAL)
        ).orElse(null);
    }

    @Transactional
    public ScanJob runScan(String regionOverride) {
        User user = currentUserService.getCurrentUserOrNull();
        String region = regionOverride != null && !regionOverride.isBlank()
                ? regionOverride
                : properties.getAws().getRegion();

        ScanJob job = new ScanJob(user, region);
        job = scanJobRepository.save(job);

        activityLogService.record(
                user,
                "SCAN_STARTED",
                "SCAN_JOB",
                String.valueOf(job.getId()),
                "Started cloud resource scan for region " + region
        );

        try {
            List<ResourceSnapshot> allSnapshots = new ArrayList<>();
            for (ResourceScanner scanner : scanners) {
                try {
                    List<ResourceSnapshot> scanned = scanner.scan(job);
                    allSnapshots.addAll(scanned);
                } catch (Exception ex) {
                    log.error("Scanner {} failed: {}", scanner.getClass().getSimpleName(), ex.getMessage(), ex);
                }
            }

            // Save snapshots and cascaded metrics
            List<ResourceSnapshot> savedSnapshots = snapshotRepository.saveAll(allSnapshots);
            job.setResourcesScannedCount(savedSnapshots.size());

            // Run Rule Engine
            List<Recommendation> recommendations = recommendationEngine.evaluate(job, savedSnapshots);
            List<Recommendation> savedRecommendations = recommendationRepository.saveAll(recommendations);

            // Generate Alerts for actionable recommendations
            for (Recommendation reco : savedRecommendations) {
                if (reco.getSeverity() == Severity.CRITICAL || reco.getSeverity() == Severity.HIGH || reco.getSeverity() == Severity.MEDIUM) {
                    alertService.createAlertFromRecommendation(job, reco);
                }
            }

            // Calculate estimated monthly cost across all discovered resources
            BigDecimal totalMonthlyCost = calculateEstimatedMonthlyCost(savedSnapshots);
            job.setEstimatedMonthlyCost(totalMonthlyCost);

            // Calculate estimated monthly savings
            BigDecimal totalSavings = savedRecommendations.stream()
                    .map(Recommendation::getEstimatedMonthlySavings)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            job.setEstimatedMonthlySavings(totalSavings);

            // Calculate Health Score
            int healthScore = calculateHealthScore(savedRecommendations);
            job.setHealthScore(healthScore);

            job.setStatus(ScanJobStatus.SUCCESS);
            job.setCompletedAt(Instant.now());
            ScanJob completedJob = scanJobRepository.save(job);

            activityLogService.record(
                    user,
                    "SCAN_COMPLETED",
                    "SCAN_JOB",
                    String.valueOf(job.getId()),
                    String.format("Scan #%d completed: %d resources, Health Score: %d/100, Est. Savings: $%s",
                            job.getId(), savedSnapshots.size(), healthScore, totalSavings.toPlainString())
            );

            return completedJob;
        } catch (Exception ex) {
            log.error("Scan job #{} failed: {}", job.getId(), ex.getMessage(), ex);
            job.setStatus(ScanJobStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(Instant.now());
            scanJobRepository.save(job);

            activityLogService.record(
                    user,
                    "SCAN_FAILED",
                    "SCAN_JOB",
                    String.valueOf(job.getId()),
                    "Scan job failed: " + ex.getMessage()
            );
            return job;
        }
    }

    private BigDecimal calculateEstimatedMonthlyCost(List<ResourceSnapshot> snapshots) {
        BigDecimal total = BigDecimal.ZERO;
        for (ResourceSnapshot snapshot : snapshots) {
            if (snapshot.getResourceType() == ResourceType.EC2) {
                String instanceType = snapshot.attribute("instanceType");
                if ("running".equalsIgnoreCase(snapshot.getState())) {
                    total = total.add(pricingCatalog.getEc2MonthlyCost(instanceType));
                }
            } else if (snapshot.getResourceType() == ResourceType.EBS) {
                double sizeGb = snapshot.attributeAsDouble("sizeGb", 0.0);
                String volType = snapshot.attribute("volumeType");
                total = total.add(pricingCatalog.getEbsMonthlyCost(sizeGb, volType));
            } else if (snapshot.getResourceType() == ResourceType.EIP) {
                if ("unassociated".equalsIgnoreCase(snapshot.getState()) || "false".equalsIgnoreCase(snapshot.attribute("associated"))) {
                    total = total.add(pricingCatalog.getUnusedEipMonthlyCost());
                }
            } else if (snapshot.getResourceType() == ResourceType.S3) {
                double sizeGb = snapshot.attributeAsDouble("estimatedSizeGb", 0.0);
                total = total.add(pricingCatalog.getS3MonthlyCost(sizeGb));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Optimization score formula (v1):
     * Score = 100 - min(100, wastePenalty)
     * wastePenalty = idle EC2 * 12 + unattached EBS * 8 + unused EIP * 6 + stale S3 * 4
     */
    private int calculateHealthScore(List<Recommendation> recommendations) {
        long idleEc2 = recommendations.stream().filter(r -> "IDLE_EC2".equals(r.getRuleCode())).count();
        long unattachedEbs = recommendations.stream().filter(r -> "UNATTACHED_EBS".equals(r.getRuleCode())).count();
        long unusedEip = recommendations.stream().filter(r -> "UNUSED_EIP".equals(r.getRuleCode())).count();
        long staleS3 = recommendations.stream().filter(r -> "STALE_S3_DATA".equals(r.getRuleCode())).count();

        long wastePenalty = (idleEc2 * 12) + (unattachedEbs * 8) + (unusedEip * 6) + (staleS3 * 4);
        int score = (int) (100 - Math.min(100, wastePenalty));
        return Math.max(0, score);
    }
}
