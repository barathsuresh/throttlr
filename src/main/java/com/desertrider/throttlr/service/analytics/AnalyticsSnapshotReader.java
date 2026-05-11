package com.desertrider.throttlr.service.analytics;

import java.util.List;

public interface AnalyticsSnapshotReader {
    List<AnalyticsSnapshot> readAll();
}
