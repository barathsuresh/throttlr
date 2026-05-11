package com.desertrider.throttlr.service.limiter;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.Rule;

@Service
public class RedisSlidingWindowRateLimiter implements SlidingWindowRateLimiter {
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List<Long>> script;

    public RedisSlidingWindowRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = redisScript("redis/sliding-window.lua");
    }

    @Override
    public CheckResponse check(Rule rule) {
        List<Long> result = redisTemplate.execute(
                script,
                List.of(redisKey(rule)),
                String.valueOf(rule.getLimitPerWindow()),
                String.valueOf(rule.getWindowMs()),
                String.valueOf(System.currentTimeMillis()),
                UUID.randomUUID().toString());

        if (result == null || result.size() != 4) {
            throw new IllegalStateException("Invalid Redis rate limit response");
        }

        boolean allowed = result.get(0) == 1;
        int remaining = Math.toIntExact(result.get(1));
        long resetAfterMs = result.get(2);
        long retryAfterMs = result.get(3);

        return new CheckResponse(allowed, remaining, resetAfterMs, retryAfterMs);
    }

    private String redisKey(Rule rule) {
        return "rl:sw:%s:%s".formatted(rule.getAppId(), rule.getClientId());
    }

    @SuppressWarnings("unchecked")
    private DefaultRedisScript<List<Long>> redisScript(String path) {
        DefaultRedisScript<List<Long>> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        redisScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
        return redisScript;
    }
}
