package com.cloudops.optimizer.dashboard;

import com.cloudops.optimizer.alert.AlertRepository;
import com.cloudops.optimizer.aws.client.AwsPricingCatalog;
import com.cloudops.optimizer.dashboard.dto.DashboardSummaryDto;
import com.cloudops.optimizer.recommendation.Recommendation;
import com.cloudops.optimizer.recommendation.RecommendationRepository;
import com.cloudops.optimizer.recommendation.RecommendationStatus;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.scan.ScanJobRepository;
import com.cloudops.optimizer.scan.ScanJobStatus;
import com.cloudops.optimizer.scan.ScanService;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceSnapshotRepository;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final ScanService scanService;
    private final ScanJobRepository scanJobRepository;
    private final ResourceSnapshotRepository snapshotRepository;
    private final RecommendationRepository recommendationRepository;
    private final AlertRepository alertRepository;
    private final AwsPricingCatalog pricingCatalog;

    public DashboardService(
            ScanService scanService,
            ScanJobRepository scanJobRepository,
            ResourceSnapshotRepository snapshotRepository,
            RecommendationRepository recommendationRepository,
            AlertRepository alertRepository,
            AwsPricingCatalog pricingCatalog) {
        this.scanService = scanService;
        this.scanJobRepository = scanJobRepository;
        this.snapshotRepository = snapshotRepository;
        this.recommendationRepository = recommendationRepository;
        this.alertRepository = alertRepository;
        this.pricingCatalog = pricingCatalog;
    }

    public DashboardSummaryDto getSummary() {
        DashboardSummaryDto dto = new DashboardSummaryDto();

        ScanJob latest = scanService.findLatestSuccessfulScan();
        List<ScanJob> recentScans = scanJobRepository.findAllByOrderByStartedAtDesc();
        dto.setRecentScans(recentScans.stream().limit(5).collect(Collectors.toList()));

        long activeAlerts = alertRepository.countByReadFalse();
        dto.setActiveAlertsCount(activeAlerts);

        if (latest == null) {
            dto.setHealthScore(100);
            dto.setTotalResources(0);
            dto.setEstimatedMonthlyCost(BigDecimal.ZERO);
            dto.setEstimatedMonthlySavings(BigDecimal.ZERO);
            dto.setOpenRecommendationsCount(0);
            dto.setResourceTypeCounts(Map.of("EC2", 0L, "EBS", 0L, "EIP", 0L, "S3", 0L, "IAM", 0L));
            dto.setCostByService(Map.of("EC2", BigDecimal.ZERO, "EBS", BigDecimal.ZERO, "EIP", BigDecimal.ZERO, "S3", BigDecimal.ZERO));
            dto.setTopRecommendations(List.of());
            return dto;
        }

        dto.setLatestScan(latest);
        dto.setHealthScore(latest.getHealthScore() != null ? latest.getHealthScore() : 100);
        dto.setEstimatedMonthlyCost(latest.getEstimatedMonthlyCost() != null ? latest.getEstimatedMonthlyCost() : BigDecimal.ZERO);
        dto.setEstimatedMonthlySavings(latest.getEstimatedMonthlySavings() != null ? latest.getEstimatedMonthlySavings() : BigDecimal.ZERO);

        List<ResourceSnapshot> snapshots = snapshotRepository.findByScanJob(latest);
        dto.setTotalResources(snapshots.size());

        // Resource type distribution
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (ResourceType type : ResourceType.values()) {
            long count = snapshots.stream().filter(s -> s.getResourceType() == type).count();
            typeCounts.put(type.name(), count);
        }
        dto.setResourceTypeCounts(typeCounts);

        // Cost breakdown by service
        Map<String, BigDecimal> costByService = calculateCostByService(snapshots);
        dto.setCostByService(costByService);

        // Recommendations
        List<Recommendation> recommendations = recommendationRepository.findByScanJobOrderByEstimatedMonthlySavingsDesc(latest);
        long openCount = recommendations.stream().filter(r -> r.getStatus() == RecommendationStatus.OPEN).count();
        dto.setOpenRecommendationsCount(openCount);
        dto.setTopRecommendations(recommendations.stream().limit(6).collect(Collectors.toList()));

        return dto;
    }

    private Map<String, BigDecimal> calculateCostByService(List<ResourceSnapshot> snapshots) {
        BigDecimal ec2Cost = BigDecimal.ZERO;
        BigDecimal ebsCost = BigDecimal.ZERO;
        BigDecimal eipCost = BigDecimal.ZERO;
        BigDecimal s3Cost = BigDecimal.ZERO;

        for (ResourceSnapshot s : snapshots) {
            if (s.getResourceType() == ResourceType.EC2 && "running".equalsIgnoreCase(s.getState())) {
                ec2Cost = ec2Cost.add(pricingCatalog.getEc2MonthlyCost(s.attribute("instanceType")));
            } else if (s.getResourceType() == ResourceType.EBS) {
                ebsCost = ebsCost.add(pricingCatalog.getEbsMonthlyCost(s.attributeAsDouble("sizeGb", 0.0), s.attribute("volumeType")));
            } else if (s.getResourceType() == ResourceType.EIP && ("unassociated".equalsIgnoreCase(s.getState()) || "false".equalsIgnoreCase(s.attribute("associated")))) {
                eipCost = eipCost.add(pricingCatalog.getUnusedEipMonthlyCost());
            } else if (s.getResourceType() == ResourceType.S3) {
                s3Cost = s3Cost.add(pricingCatalog.getS3MonthlyCost(s.attributeAsDouble("estimatedSizeGb", 0.0)));
            }
        }

        Map<String, BigDecimal> map = new LinkedHashMap<>();
        map.put("EC2 Compute", ec2Cost.setScale(2, RoundingMode.HALF_UP));
        map.put("EBS Storage", ebsCost.setScale(2, RoundingMode.HALF_UP));
        map.put("Elastic IPs", eipCost.setScale(2, RoundingMode.HALF_UP));
        map.put("S3 Storage", s3Cost.setScale(2, RoundingMode.HALF_UP));
        return map;
    }
}
