package com.desertrider.throttlr.service.cache;

import java.time.Duration;
import java.util.Optional;

import com.desertrider.throttlr.model.App;

public interface AppKeyCacheService {
    Optional<App> findByLookup(String lookup);

    void put(App app);

    default void put(App app, Duration ttl) {
        put(app);
    }

    default void delete(String lookup) {
    }
}
