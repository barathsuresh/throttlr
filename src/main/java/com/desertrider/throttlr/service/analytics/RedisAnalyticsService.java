package com.desertrider.throttlr.service.analytics;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.App;

@Service
public class RedisAnalyticsService implements AnalyticsService {
    private static final Logger log = LoggerFactory.getLogger(RedisAnalyticsService.class);
    private static final Duration ANALYTICS_TTL = Duration.ofDays(2);

    private final RedisTemplate<String, String> redisTemplate;

    public RedisAnalyticsService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void record(App app, String clientId, CheckResponse response) {
        try {
            String key = redisKey(app.getId());
            String decisionField = response.allowed() ? "allowed" : "blocked";

            redisTemplate.opsForHash().increment(key, "total", 1);
            redisTemplate.opsForHash().increment(key, decisionField, 1);
            redisTemplate.opsForHash().increment(key, "client:" + clientId, 1);
            redisTemplate.expire(key, ANALYTICS_TTL);
        } catch (Exception e) {
            log.warn("[ANALYTICS] Failed to record analytics - appId: [{}]", app.getId(), e);
        }
    }

    private String redisKey(String appId) {
        long hour = Instant.now().truncatedTo(ChronoUnit.HOURS).toEpochMilli();
        return "analytics:%s:%s".formatted(appId, hour);
    }
}
