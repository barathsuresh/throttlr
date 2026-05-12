package com.desertrider.throttlr.service.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.desertrider.throttlr.model.Rule;

public interface RuleCacheService {
    Optional<Rule> findByAppIdAndClientId(String appId, String clientId);

    List<Rule> findPatternsByAppId(String appId);

    void put(Rule rule);

    default void put(Rule rule, Duration ttl) {
        put(rule);
    }

    void delete(String appId, String clientId);

    void deletePatternCache(String appId);
}
