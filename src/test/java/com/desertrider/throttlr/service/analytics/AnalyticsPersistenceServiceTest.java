package com.desertrider.throttlr.service.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class AnalyticsPersistenceServiceTest {
    @Test
    void flushPersistsEveryRedisSnapshotToMongo() {
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot("app-1", 1_770_000_000_000L, 12, 10, 2);
        RecordingSnapshotReader reader = new RecordingSnapshotReader(List.of(snapshot));
        RecordingSnapshotWriter writer = new RecordingSnapshotWriter();
        AnalyticsPersistenceService service = new AnalyticsPersistenceService(reader, writer, 30);

        service.flushRedisAnalyticsToMongo();

        assertEquals(List.of(snapshot), writer.savedSnapshots);
    }

    @Test
    void cleanupDeletesAnalyticsOlderThanRetentionWindow() {
        RecordingSnapshotReader reader = new RecordingSnapshotReader(List.of());
        RecordingSnapshotWriter writer = new RecordingSnapshotWriter();
        AnalyticsPersistenceService service = new AnalyticsPersistenceService(reader, writer, 30);

        service.cleanupOldMongoAnalytics(1_770_000_000_000L);

        assertEquals(1_767_405_600_000L, writer.deletedBeforeHour);
    }

    private record RecordingSnapshotReader(List<AnalyticsSnapshot> snapshots) implements AnalyticsSnapshotReader {
        @Override
        public List<AnalyticsSnapshot> readAll() {
            return snapshots;
        }
    }

    private static final class RecordingSnapshotWriter implements AnalyticsSnapshotWriter {
        private List<AnalyticsSnapshot> savedSnapshots = List.of();
        private Long deletedBeforeHour;

        @Override
        public void upsertAll(List<AnalyticsSnapshot> snapshots) {
            savedSnapshots = snapshots;
        }

        @Override
        public long deleteOlderThan(long cutoffHour) {
            deletedBeforeHour = cutoffHour;
            return 0;
        }
    }
}
