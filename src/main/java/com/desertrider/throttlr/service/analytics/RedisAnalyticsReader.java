package com.desertrider.throttlr.service.analytics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisAnalyticsReader implements AnalyticsReader {
    private final RedisTemplate<String, String> redisTemplate;

    public RedisAnalyticsReader(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Map<String, Long> getCurrentHourCounters(String appId) {
        String key = redisKey(appId, currentHour());

        return Map.of(
                "total", readCounter(key, "total"),
                "allowed", readCounter(key, "allowed"),
                "blocked", readCounter(key, "blocked"));
    }

    @Override
    public long currentHour() {
        return Instant.now().truncatedTo(ChronoUnit.HOURS).toEpochMilli();
    }

    private long readCounter(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    private String redisKey(String appId, long hour) {
        return "analytics:%s:%s".formatted(appId, hour);
    }
}
