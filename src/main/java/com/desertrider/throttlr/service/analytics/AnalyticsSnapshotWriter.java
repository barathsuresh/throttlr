package com.desertrider.throttlr.service.analytics;

import java.util.List;

public interface AnalyticsSnapshotWriter {
    void upsertAll(List<AnalyticsSnapshot> snapshots);

    long deleteOlderThan(long cutoffHour);
}
