package com.cloudops.optimizer.scan;

import com.cloudops.optimizer.config.OptimizerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScanScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScanScheduler.class);

    private final ScanService scanService;
    private final OptimizerProperties properties;

    public ScanScheduler(ScanService scanService, OptimizerProperties properties) {
        this.scanService = scanService;
        this.properties = properties;
    }

    @Scheduled(cron = "${optimizer.scan.cron:0 0 2 * * *}")
    public void runDailyScheduledScan() {
        if (!properties.getScan().isEnabled()) {
            return;
        }
        log.info("Triggering scheduled automated AWS cost optimization scan");
        try {
            scanService.runScan(properties.getAws().getRegion());
        } catch (Exception ex) {
            log.error("Scheduled scan failed: {}", ex.getMessage(), ex);
        }
    }
}
