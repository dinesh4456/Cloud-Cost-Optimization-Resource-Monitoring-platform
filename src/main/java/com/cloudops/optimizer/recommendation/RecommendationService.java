package com.cloudops.optimizer.recommendation;

import com.cloudops.optimizer.activity.ActivityLogService;
import com.cloudops.optimizer.common.CurrentUserService;
import com.cloudops.optimizer.common.ResourceNotFoundException;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;

    public RecommendationService(
            RecommendationRepository recommendationRepository,
            ActivityLogService activityLogService,
            CurrentUserService currentUserService) {
        this.recommendationRepository = recommendationRepository;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;
    }

    public List<Recommendation> findAll() {
        return recommendationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Recommendation> findByScanJob(ScanJob scanJob) {
        return recommendationRepository.findByScanJobOrderByEstimatedMonthlySavingsDesc(scanJob);
    }

    public Recommendation findById(Long id) {
        return recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + id));
    }

    @Transactional
    public Recommendation updateStatus(Long id, RecommendationStatus newStatus) {
        Recommendation recommendation = findById(id);
        recommendation.setStatus(newStatus);
        Recommendation saved = recommendationRepository.save(recommendation);

        User currentUser = currentUserService.getCurrentUserOrNull();
        activityLogService.record(
                currentUser,
                "RECOMMENDATION_" + newStatus.name(),
                "RECOMMENDATION",
                String.valueOf(id),
                "Updated status of recommendation '" + recommendation.getTitle() + "' to " + newStatus.name()
        );

        return saved;
    }
}
