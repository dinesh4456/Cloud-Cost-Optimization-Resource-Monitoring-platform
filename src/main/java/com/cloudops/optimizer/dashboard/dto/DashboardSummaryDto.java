package com.cloudops.optimizer.dashboard.dto;

import com.cloudops.optimizer.recommendation.Recommendation;
import com.cloudops.optimizer.scan.ScanJob;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardSummaryDto {

    private Integer healthScore;
    private int totalResources;
    private BigDecimal estimatedMonthlyCost;
    private BigDecimal estimatedMonthlySavings;
    private long activeAlertsCount;
    private long openRecommendationsCount;
    private Map<String, Long> resourceTypeCounts;
    private Map<String, BigDecimal> costByService;
    private ScanJob latestScan;
    private List<ScanJob> recentScans;
    private List<Recommendation> topRecommendations;

    public Integer getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Integer healthScore) {
        this.healthScore = healthScore;
    }

    public int getTotalResources() {
        return totalResources;
    }

    public void setTotalResources(int totalResources) {
        this.totalResources = totalResources;
    }

    public BigDecimal getEstimatedMonthlyCost() {
        return estimatedMonthlyCost;
    }

    public void setEstimatedMonthlyCost(BigDecimal estimatedMonthlyCost) {
        this.estimatedMonthlyCost = estimatedMonthlyCost;
    }

    public BigDecimal getEstimatedMonthlySavings() {
        return estimatedMonthlySavings;
    }

    public void setEstimatedMonthlySavings(BigDecimal estimatedMonthlySavings) {
        this.estimatedMonthlySavings = estimatedMonthlySavings;
    }

    public long getActiveAlertsCount() {
        return activeAlertsCount;
    }

    public void setActiveAlertsCount(long activeAlertsCount) {
        this.activeAlertsCount = activeAlertsCount;
    }

    public long getOpenRecommendationsCount() {
        return openRecommendationsCount;
    }

    public void setOpenRecommendationsCount(long openRecommendationsCount) {
        this.openRecommendationsCount = openRecommendationsCount;
    }

    public Map<String, Long> getResourceTypeCounts() {
        return resourceTypeCounts;
    }

    public void setResourceTypeCounts(Map<String, Long> resourceTypeCounts) {
        this.resourceTypeCounts = resourceTypeCounts;
    }

    public Map<String, BigDecimal> getCostByService() {
        return costByService;
    }

    public void setCostByService(Map<String, BigDecimal> costByService) {
        this.costByService = costByService;
    }

    public ScanJob getLatestScan() {
        return latestScan;
    }

    public void setLatestScan(ScanJob latestScan) {
        this.latestScan = latestScan;
    }

    public List<ScanJob> getRecentScans() {
        return recentScans;
    }

    public void setRecentScans(List<ScanJob> recentScans) {
        this.recentScans = recentScans;
    }

    public List<Recommendation> getTopRecommendations() {
        return topRecommendations;
    }

    public void setTopRecommendations(List<Recommendation> topRecommendations) {
        this.topRecommendations = topRecommendations;
    }
}
