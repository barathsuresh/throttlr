package com.desertrider.throttlr.service.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.desertrider.throttlr.model.Rule;

/**
 * Caches rate limit rules for fast lookup during rate limit checks.
 * Supports exact-match rules and glob patterns.
 * Implementation typically uses Redis for performance.
 */
public interface RuleCacheService {
    /** Looks up exact-match rule for (app, client). */
    Optional<Rule> findByAppIdAndClientId(String appId, String clientId);

    /** Lists all pattern rules for app (clientId contains *). */
    List<Rule> findPatternsByAppId(String appId);

    /** Caches rule. Optional TTL parameter with default implementation. */
    void put(Rule rule);

    default void put(Rule rule, Duration ttl) {
        put(rule);
    }

    /** Removes exact-match rule from cache. */
    void delete(String appId, String clientId);

    /** Clears pattern cache for app (called after rule updates). */
    void deletePatternCache(String appId);
}
