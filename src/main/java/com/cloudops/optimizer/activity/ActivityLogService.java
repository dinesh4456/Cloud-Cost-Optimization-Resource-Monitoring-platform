package com.cloudops.optimizer.activity;

import com.cloudops.optimizer.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void record(User user, String actionType, String entityType, String entityId, String details) {
        activityLogRepository.save(new ActivityLog(user, actionType, entityType, entityId, details));
        log.info("activity action={} entity={} id={}", actionType, entityType, entityId);
    }
}
