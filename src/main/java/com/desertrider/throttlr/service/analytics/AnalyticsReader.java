package com.desertrider.throttlr.service.analytics;

import java.util.Map;

public interface AnalyticsReader {
    Map<String, Long> getCurrentHourCounters(String appId);

    long currentHour();
}
