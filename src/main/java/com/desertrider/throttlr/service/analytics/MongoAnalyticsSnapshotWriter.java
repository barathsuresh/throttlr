package com.desertrider.throttlr.service.analytics;

import java.util.List;

import org.springframework.stereotype.Service;

import com.desertrider.throttlr.model.Analytics;
import com.desertrider.throttlr.repository.AnalyticsRepository;

@Service
public class MongoAnalyticsSnapshotWriter implements AnalyticsSnapshotWriter {
    private final AnalyticsRepository analyticsRepository;

    public MongoAnalyticsSnapshotWriter(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public void upsertAll(List<AnalyticsSnapshot> snapshots) {
        snapshots.forEach(this::upsert);
    }

    @Override
    public long deleteOlderThan(long cutoffHour) {
        return analyticsRepository.deleteByHourLessThan(cutoffHour);
    }

    private void upsert(AnalyticsSnapshot snapshot) {
        long now = System.currentTimeMillis();
        Analytics analytics = analyticsRepository.findByAppIdAndHour(snapshot.appId(), snapshot.hour())
                .orElseGet(() -> Analytics.builder()
                        .appId(snapshot.appId())
                        .hour(snapshot.hour())
                        .createdAt(now)
                        .build());

        analytics.setTotalRequests(snapshot.totalRequests());
        analytics.setAllowedRequests(snapshot.allowedRequests());
        analytics.setBlockedRequests(snapshot.blockedRequests());
        analytics.setUpdatedAt(now);

        analyticsRepository.save(analytics);
    }
}
