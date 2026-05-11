package com.desertrider.throttlr.service.analytics;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsPersistenceScheduler {
    private final AnalyticsPersistenceService analyticsPersistenceService;

    public AnalyticsPersistenceScheduler(AnalyticsPersistenceService analyticsPersistenceService) {
        this.analyticsPersistenceService = analyticsPersistenceService;
    }

    @Scheduled(cron = "${app.analytics.flush-cron:0 0 * * * *}")
    public void flushRedisAnalyticsToMongo() {
        analyticsPersistenceService.flushRedisAnalyticsToMongo();
    }

    @Scheduled(cron = "${app.analytics.cleanup-cron:0 30 2 * * *}")
    public void cleanupOldMongoAnalytics() {
        analyticsPersistenceService.cleanupOldMongoAnalytics();
    }
}
