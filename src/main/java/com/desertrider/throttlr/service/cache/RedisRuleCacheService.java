package com.desertrider.throttlr.service.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.desertrider.throttlr.model.Rule;
import com.desertrider.throttlr.repository.RuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RedisRuleCacheService implements RuleCacheService {
    private static final String RULE_KEY_PREFIX = "rule:";
    private static final String PATTERN_KEY_PREFIX = "rule-patterns:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RuleRepository ruleRepository;
    private final Duration ttl;

    public RedisRuleCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            RuleRepository ruleRepository,
            @Value("${app.cache.rule-ttl-ms:300000}") long ttlMs) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ruleRepository = ruleRepository;
        this.ttl = Duration.ofMillis(ttlMs);
    }

    @Override
    public Optional<Rule> findByAppIdAndClientId(String appId, String clientId) {
        return readFromRedis(appId, clientId)
                .or(() -> ruleRepository.findByAppIdAndClientId(appId, clientId).map(rule -> {
                    put(rule);
                    return rule;
                }));
    }

    @Override
    public List<Rule> findPatternsByAppId(String appId) {
        try {
            String value = redisTemplate.opsForValue().get(patternKey(appId));
            if (value != null) {
                return objectMapper.readValue(value, new TypeReference<List<Rule>>() {});
            }
        } catch (Exception e) {
            log.warn("[CACHE] Failed to read pattern cache from Redis - appId: [{}]", appId, e);
        }
        List<Rule> patterns = ruleRepository.findPatternsByAppId(appId);
        cachePatterns(appId, patterns);
        return patterns;
    }

    @Override
    public void put(Rule rule) {
        put(rule, ttl);
    }

    @Override
    public void put(Rule rule, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(
                    ruleKey(rule.getAppId(), rule.getClientId()),
                    objectMapper.writeValueAsString(rule),
                    ttl);
        } catch (Exception e) {
            log.warn("[CACHE] Failed to write rule to Redis - appId: [{}], clientId: [{}]",
                    rule.getAppId(), rule.getClientId(), e);
        }
    }

    @Override
    public void delete(String appId, String clientId) {
        try {
            redisTemplate.delete(ruleKey(appId, clientId));
        } catch (Exception e) {
            log.warn("[CACHE] Failed to delete rule from Redis - appId: [{}], clientId: [{}]", appId, clientId, e);
        }
    }

    @Override
    public void deletePatternCache(String appId) {
        try {
            redisTemplate.delete(patternKey(appId));
        } catch (Exception e) {
            log.warn("[CACHE] Failed to delete pattern cache from Redis - appId: [{}]", appId, e);
        }
    }

    private Optional<Rule> readFromRedis(String appId, String clientId) {
        try {
            String value = redisTemplate.opsForValue().get(ruleKey(appId, clientId));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, Rule.class));
        } catch (Exception e) {
            log.warn("[CACHE] Failed to read rule from Redis - appId: [{}], clientId: [{}]", appId, clientId, e);
            return Optional.empty();
        }
    }

    private void cachePatterns(String appId, List<Rule> patterns) {
        try {
            redisTemplate.opsForValue().set(
                    patternKey(appId),
                    objectMapper.writeValueAsString(patterns),
                    ttl);
        } catch (Exception e) {
            log.warn("[CACHE] Failed to cache patterns in Redis - appId: [{}]", appId, e);
        }
    }

    private String ruleKey(String appId, String clientId) {
        return RULE_KEY_PREFIX + appId + ":" + clientId;
    }

    private String patternKey(String appId) {
        return PATTERN_KEY_PREFIX + appId;
    }
}
