package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.service.limiter.RedisSlidingWindowRateLimiter;

class RedisSlidingWindowRateLimiterTest {
    @Test
    void checkMapsRedisScriptResultToCheckResponse() {
        RecordingRedisTemplate redisTemplate = new RecordingRedisTemplate(List.of(0L, 0L, 12_000L, 12_000L));
        RedisSlidingWindowRateLimiter limiter = new RedisSlidingWindowRateLimiter(redisTemplate);

        CheckResponse response = limiter.check(Rule.builder()
                .appId("app-1")
                .clientId("user:123")
                .limitPerWindow(100)
                .windowMs(60_000)
                .build());

        assertFalse(response.allowed());
        assertEquals(0, response.remaining());
        assertEquals(12_000, response.resetAfterMs());
        assertEquals(12_000, response.retryAfterMs());
        assertEquals(List.of("rl:sw:app-1:user:123"), redisTemplate.keys);
        assertEquals("100", redisTemplate.args[0]);
        assertEquals("60000", redisTemplate.args[1]);
    }

    private static final class RecordingRedisTemplate extends RedisTemplate<String, String> {
        private final List<Long> result;
        private List<String> keys;
        private Object[] args;

        private RecordingRedisTemplate(List<Long> result) {
            this.result = result;
        }

        @Override
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            this.keys = keys;
            this.args = args;
            return script.getResultType().cast(result);
        }
    }
}
