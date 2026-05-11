package com.desertrider.throttlr.security.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(OutputCaptureExtension.class)
class RedisPublicRateLimitServiceTest {
    private final PublicRateLimitPolicy policy = new PublicRateLimitPolicy("login", 10, Duration.ofMinutes(1));

    @Test
    void checkAllowsWhenRedisScriptAllows() {
        RedisPublicRateLimitService service = new RedisPublicRateLimitService(new FakeRedisTemplate(List.of(1L, 9L, 0L)));

        PublicRateLimitDecision decision = service.check(policy, "192.168.1.10");

        assertTrue(decision.allowed());
        assertEquals(Duration.ZERO, decision.retryAfter());
    }

    @Test
    void checkBlocksWhenRedisScriptBlocks() {
        RedisPublicRateLimitService service = new RedisPublicRateLimitService(new FakeRedisTemplate(List.of(0L, 0L, 30_000L)));

        PublicRateLimitDecision decision = service.check(policy, "192.168.1.10");

        assertFalse(decision.allowed());
        assertEquals(Duration.ofSeconds(30), decision.retryAfter());
    }

    @Test
    void checkFailsOpenWhenRedisErrors() {
        RedisPublicRateLimitService service = new RedisPublicRateLimitService(new FakeRedisTemplate(new RuntimeException("redis down")));

        PublicRateLimitDecision decision = service.check(policy, "192.168.1.10");

        assertTrue(decision.allowed());
    }

    private static class FakeRedisTemplate extends RedisTemplate<String, String> {
        private final Object response;

        private FakeRedisTemplate(Object response) {
            this.response = response;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            if (response instanceof RuntimeException exception) {
                throw exception;
            }

            T typedResponse = (T) response;
            return typedResponse;
        }
    }
}
