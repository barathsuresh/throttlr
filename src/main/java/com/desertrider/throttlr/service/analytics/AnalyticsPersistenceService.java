package com.desertrider.throttlr.service.analytics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsPersistenceService {
    private final AnalyticsSnapshotReader snapshotReader;
    private final AnalyticsSnapshotWriter snapshotWriter;
    private final int retentionDays;

    public AnalyticsPersistenceService(
            AnalyticsSnapshotReader snapshotReader,
            AnalyticsSnapshotWriter snapshotWriter,
            @Value("${app.analytics.retention-days:30}") int retentionDays) {
        this.snapshotReader = snapshotReader;
        this.snapshotWriter = snapshotWriter;
        this.retentionDays = retentionDays;
    }

    public void flushRedisAnalyticsToMongo() {
        List<AnalyticsSnapshot> snapshots = snapshotReader.readAll();
        if (!snapshots.isEmpty()) {
            snapshotWriter.upsertAll(snapshots);
        }
    }

    public long cleanupOldMongoAnalytics() {
        return cleanupOldMongoAnalytics(System.currentTimeMillis());
    }

    long cleanupOldMongoAnalytics(long nowMs) {
        long cutoffHour = Instant.ofEpochMilli(nowMs)
                .minus(retentionDays, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.HOURS)
                .toEpochMilli();

        return snapshotWriter.deleteOlderThan(cutoffHour);
    }
}
