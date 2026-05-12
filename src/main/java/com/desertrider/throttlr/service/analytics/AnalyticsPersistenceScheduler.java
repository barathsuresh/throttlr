package com.desertrider.throttlr.service.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsPersistenceScheduler {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsPersistenceScheduler.class);
    private final AnalyticsPersistenceService analyticsPersistenceService;

    public AnalyticsPersistenceScheduler(AnalyticsPersistenceService analyticsPersistenceService) {
        this.analyticsPersistenceService = analyticsPersistenceService;
    }

    @Scheduled(cron = "${app.analytics.flush-cron:0 0 * * * *}")
    public void flushRedisAnalyticsToMongo() {
        log.info("[ANALYTICS] Flush started - Redis to Mongo");
        analyticsPersistenceService.flushRedisAnalyticsToMongo();
        log.info("[ANALYTICS] Flush complete - Redis to Mongo");
    }

    @Scheduled(cron = "${app.analytics.cleanup-cron:0 30 2 * * *}")
    public void cleanupOldMongoAnalytics() {
        log.info("[ANALYTICS] Cleanup started - old Mongo analytics");
        analyticsPersistenceService.cleanupOldMongoAnalytics();
        log.info("[ANALYTICS] Cleanup complete - old Mongo analytics");
    }
}
