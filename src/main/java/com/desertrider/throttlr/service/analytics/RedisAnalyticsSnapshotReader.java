package com.desertrider.throttlr.service.analytics;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

@Service
public class RedisAnalyticsSnapshotReader implements AnalyticsSnapshotReader {
    private static final String ANALYTICS_KEY_PATTERN = "analytics:*";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisAnalyticsSnapshotReader(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<AnalyticsSnapshot> readAll() {
        List<String> keys = scanAnalyticsKeys();
        if (keys.isEmpty()) {
            return List.of();
        }

        return keys.stream()
                .map(this::toSnapshot)
                .flatMap(List::stream)
                .toList();
    }

    private List<String> scanAnalyticsKeys() {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(ANALYTICS_KEY_PATTERN)
                .count(1_000)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }

        return keys;
    }

    private List<AnalyticsSnapshot> toSnapshot(String key) {
        String[] parts = key.split(":");
        if (parts.length != 3) {
            return List.of();
        }

        String appId = parts[1];
        long hour = Long.parseLong(parts[2]);

        return List.of(new AnalyticsSnapshot(
                appId,
                hour,
                readCounter(key, "total"),
                readCounter(key, "allowed"),
                readCounter(key, "blocked")));
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
}
