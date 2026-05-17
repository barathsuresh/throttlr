package com.desertrider.throttlr.service.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.App;

class RedisAnalyticsServiceTest {
    @Test
    void recordPipelinesAnalyticsCommandsInSingleRedisRoundTrip() {
        RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate();

        RedisAnalyticsService service = new RedisAnalyticsService(redisTemplate);

        service.record(App.builder().id("app-1").build(), "client-1", new CheckResponse(true, 9, 1000, 0));

        assertEquals(1, redisTemplate.pipelinedCalls);
        assertTrue(redisTemplate.increments.stream().anyMatch(call -> call.field().equals("total")));
        assertTrue(redisTemplate.increments.stream().anyMatch(call -> call.field().equals("allowed")));
        assertTrue(redisTemplate.increments.stream().anyMatch(call -> call.field().equals("client:client-1")));
        assertEquals(Duration.ofDays(2), redisTemplate.expireTtl);
    }

    private record IncrementCall(String key, Object field, long delta) {
    }

    private static final class RecordingRedisTemplate extends RedisTemplate<String, String> {
        private int pipelinedCalls;
        private final List<IncrementCall> increments = new ArrayList<>();
        private Duration expireTtl;

        @Override
        @SuppressWarnings("unchecked")
        public List<Object> executePipelined(SessionCallback<?> session) {
            pipelinedCalls++;
            session.execute(recordingOperations());
            return List.of();
        }

        @SuppressWarnings("unchecked")
        private RedisOperations<String, String> recordingOperations() {
            return (RedisOperations<String, String>) Proxy.newProxyInstance(
                    RedisOperations.class.getClassLoader(),
                    new Class<?>[] { RedisOperations.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "opsForHash" -> recordingHashOperations();
                        case "expire" -> {
                            expireTtl = (Duration) args[1];
                            yield Boolean.TRUE;
                        }
                        case "toString" -> "RecordingRedisOperations";
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private Object recordingHashOperations() {
            return Proxy.newProxyInstance(
                    RedisOperations.class.getClassLoader(),
                    new Class<?>[] { org.springframework.data.redis.core.HashOperations.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "increment" -> {
                            increments.add(new IncrementCall((String) args[0], args[1], (Long) args[2]));
                            yield 1L;
                        }
                        case "toString" -> "RecordingHashOperations";
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == int.class) {
                return 0;
            }
            return null;
        }
    }
}
