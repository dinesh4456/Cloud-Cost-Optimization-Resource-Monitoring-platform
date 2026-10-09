package com.cloudops.optimizer.alert;

import com.cloudops.optimizer.activity.ActivityLogService;
import com.cloudops.optimizer.common.CurrentUserService;
import com.cloudops.optimizer.common.ResourceNotFoundException;
import com.cloudops.optimizer.recommendation.Recommendation;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;

    public AlertService(
            AlertRepository alertRepository,
            ActivityLogService activityLogService,
            CurrentUserService currentUserService) {
        this.alertRepository = alertRepository;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;
    }

    public List<Alert> findAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    public long countUnread() {
        return alertRepository.countByReadFalse();
    }

    public Alert findById(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
    }

    @Transactional
    public Alert createAlertFromRecommendation(ScanJob scanJob, Recommendation recommendation) {
        String alertType = recommendation.getRuleCode() + "_ALERT";
        String message = recommendation.getTitle() + " - Est. Monthly Savings: $" + recommendation.getEstimatedMonthlySavings();
        Alert alert = new Alert(scanJob, recommendation, alertType, recommendation.getSeverity(), message);
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert markAsRead(Long id) {
        Alert alert = findById(id);
        alert.setRead(true);
        Alert saved = alertRepository.save(alert);

        User user = currentUserService.getCurrentUserOrNull();
        activityLogService.record(
                user,
                "ALERT_READ",
                "ALERT",
                String.valueOf(id),
                "Marked alert " + id + " as read"
        );
        return saved;
    }

    @Transactional
    public void markAllAsRead() {
        List<Alert> alerts = alertRepository.findAll();
        for (Alert a : alerts) {
            a.setRead(true);
        }
        alertRepository.saveAll(alerts);

        User user = currentUserService.getCurrentUserOrNull();
        activityLogService.record(
                user,
                "ALERTS_ALL_READ",
                "ALERT",
                "ALL",
                "Marked all alerts as read"
        );
    }
}
