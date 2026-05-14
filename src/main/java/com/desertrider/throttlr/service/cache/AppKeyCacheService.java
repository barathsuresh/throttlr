package com.desertrider.throttlr.service.cache;

import java.time.Duration;
import java.util.Optional;

import com.desertrider.throttlr.model.App;

/**
 * Caches app configurations using API key lookup hash for O(1) access.
 * Enables fast app validation during rate limit checks.
 * Implementation typically uses Redis or in-memory cache.
 */
public interface AppKeyCacheService {
    /** Finds app by HMAC lookup hash. */
    Optional<App> findByLookup(String lookup);

    /** Caches app with optional TTL. */
    void put(App app);

    default void put(App app, Duration ttl) {
        put(app);
    }

    /** Removes app from cache by lookup hash. */
    default void delete(String lookup) {
    }
}
