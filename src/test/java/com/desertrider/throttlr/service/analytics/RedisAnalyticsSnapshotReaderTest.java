package com.desertrider.throttlr.service.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.Cursor.CursorId;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

class RedisAnalyticsSnapshotReaderTest {
    @Test
    void readAllScansAnalyticsKeysAndMapsCounters() {
        RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate(List.of("analytics:app-1:1000"));
        redisTemplate.hashValues.put("analytics:app-1:1000:total", "7");
        redisTemplate.hashValues.put("analytics:app-1:1000:allowed", "5");
        redisTemplate.hashValues.put("analytics:app-1:1000:blocked", "2");
        RedisAnalyticsSnapshotReader reader = new RedisAnalyticsSnapshotReader(redisTemplate);

        List<AnalyticsSnapshot> snapshots = reader.readAll();

        assertEquals(List.of(new AnalyticsSnapshot("app-1", 1000, 7, 5, 2)), snapshots);
        assertEquals(1, redisTemplate.scanCount);
    }

    private static final class RecordingRedisTemplate extends RedisTemplate<String, String> {
        private final Map<String, String> hashValues = new HashMap<>();
        private final List<String> keys;
        private int scanCount;

        private RecordingRedisTemplate(List<String> keys) {
            this.keys = keys;
        }

        @Override
        public Cursor<String> scan(ScanOptions options) {
            scanCount++;
            return cursor(keys);
        }

        @Override
        public <HK, HV> HashOperations<String, HK, HV> opsForHash() {
            return hashOperations(hashValues);
        }
    }

    @SuppressWarnings("unchecked")
    private static Cursor<String> cursor(List<String> keys) {
        Iterator<String> iterator = keys.iterator();

        return (Cursor<String>) Proxy.newProxyInstance(
                Cursor.class.getClassLoader(),
                new Class<?>[] { Cursor.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasNext" -> iterator.hasNext();
                    case "next" -> iterator.next();
                    case "close" -> null;
                    case "isClosed" -> false;
                    case "getCursorId", "getPosition" -> 0L;
                    case "getId" -> CursorId.initial();
                    case "toString" -> "RecordingCursor";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @SuppressWarnings("unchecked")
    private static <HK, HV> HashOperations<String, HK, HV> hashOperations(Map<String, String> hashValues) {
        return (HashOperations<String, HK, HV>) Proxy.newProxyInstance(
                HashOperations.class.getClassLoader(),
                new Class<?>[] { HashOperations.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "get" -> hashValues.get(args[0] + ":" + args[1]);
                    case "toString" -> "RecordingHashOperations";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
