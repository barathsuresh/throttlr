package com.desertrider.throttlr.service.limiter;

import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import com.desertrider.throttlr.dto.response.CheckResponse;
import com.desertrider.throttlr.model.Rule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RedisFixedWindowRateLimiter implements FixedWindowRateLimiter {
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List<Long>> script;

    public RedisFixedWindowRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = redisScript("redis/fixed-window.lua");
    }

    @Override
    public CheckResponse check(Rule rule) {
        List<Long> result = redisTemplate.execute(
                script,
                List.of(redisKey(rule)),
                String.valueOf(rule.getLimitPerWindow()),
                String.valueOf(rule.getWindowMs()));

        if (result == null || result.size() != 4) {
            log.error("[RATE-LIMIT] Invalid response from Redis fixed-window script - appId: [{}], clientId: [{}], result: {}",
                    rule.getAppId(), rule.getClientId(), result);
            throw new IllegalStateException("Invalid Redis rate limit response");
        }

        boolean allowed = result.get(0) == 1;
        int remaining = Math.toIntExact(result.get(1));
        long resetAfterMs = result.get(2);
        long retryAfterMs = result.get(3);

        return new CheckResponse(allowed, remaining, resetAfterMs, retryAfterMs);
    }

    private String redisKey(Rule rule) {
        return "rl:%s:%s".formatted(rule.getAppId(), rule.getClientId());
    }

    @SuppressWarnings("unchecked")
    private DefaultRedisScript<List<Long>> redisScript(String path) {
        DefaultRedisScript<List<Long>> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        redisScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
        return redisScript;
    }
}
