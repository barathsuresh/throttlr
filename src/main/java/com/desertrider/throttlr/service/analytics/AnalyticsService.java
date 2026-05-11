package com.desertrider.throttlr.service.analytics;

import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.App;

public interface AnalyticsService {
    void record(App app, String clientId, CheckResponse response);
}
