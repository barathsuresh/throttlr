package com.desertrider.throttlr.security.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisPublicRateLimitService implements PublicRateLimitService {
    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<List<Long>> script;

    public RedisPublicRateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = redisScript("redis/public-rate-limit.lua");
    }

    @Override
    public PublicRateLimitDecision check(PublicRateLimitPolicy policy, String clientIp) {
        try {
            List<Long> result = redisTemplate.execute(
                    script,
                    List.of(redisKey(policy, clientIp)),
                    String.valueOf(policy.maxRequests()),
                    String.valueOf(policy.window().toMillis()));

            if (result == null || result.size() != 3) {
                log.warn("Invalid public rate limit Redis response for policy {}", policy.name());
                return PublicRateLimitDecision.allow();
            }

            boolean allowed = result.get(0) == 1;
            long retryAfterMs = result.get(2);

            if (allowed) {
                return PublicRateLimitDecision.allow();
            }

            return PublicRateLimitDecision.blocked(Duration.ofMillis(retryAfterMs));
        } catch (Exception ex) {
            log.warn("Public rate limit check failed for policy {}: {}", policy.name(), ex.getMessage());
            return PublicRateLimitDecision.allow();
        }
    }

    private String redisKey(PublicRateLimitPolicy policy, String clientIp) {
        return "public-rl:%s:%s".formatted(policy.name(), sha256(clientIp));
    }

    @SuppressWarnings("unchecked")
    private DefaultRedisScript<List<Long>> redisScript(String path) {
        DefaultRedisScript<List<Long>> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        redisScript.setResultType((Class<List<Long>>) (Class<?>) List.class);
        return redisScript;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash rate limit key", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

}
