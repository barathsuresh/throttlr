package com.desertrider.throttlr.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.desertrider.throttlr.dto.response.AnalyticsResponse;
import com.desertrider.throttlr.exception.ResourceNotFoundException;
import com.desertrider.throttlr.repository.AppRepository;
import com.desertrider.throttlr.service.analytics.AnalyticsReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsReadService {
    private final AppRepository appRepository;
    private final AnalyticsReader analyticsReader;

    public AnalyticsResponse getCurrentHourAnalytics(String accountId, String appId) {
        appRepository.findByIdAndAccountId(appId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("App not found"));

        Map<String, Long> counters = analyticsReader.getCurrentHourCounters(appId);

        return new AnalyticsResponse(
                appId,
                analyticsReader.currentHour(),
                counters.getOrDefault("total", 0L),
                counters.getOrDefault("allowed", 0L),
                counters.getOrDefault("blocked", 0L));
    }
}
