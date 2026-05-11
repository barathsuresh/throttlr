package com.desertrider.throttlr.service.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.repository.AppRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisAppKeyCacheService implements AppKeyCacheService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppRepository appRepository;
    private final Duration ttl;

    public RedisAppKeyCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            AppRepository appRepository,
            @Value("${app.cache.app-key-ttl-ms:300000}") long ttlMs) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.appRepository = appRepository;
        this.ttl = Duration.ofMillis(ttlMs);
    }

    @Override
    public Optional<App> findByLookup(String lookup) {
        return readFromRedis(lookup)
                .or(() -> appRepository.findByApiKeyLookup(lookup).map(app -> {
                    put(app);
                    return app;
                }));
    }

    @Override
    public void put(App app) {
        put(app, ttl);
    }

    @Override
    public void put(App app, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(redisKey(app.getApiKeyLookup()), objectMapper.writeValueAsString(app), ttl);
        } catch (Exception ignored) {
            // Cache failures should not break the control plane or runtime auth path.
        }
    }

    @Override
    public void delete(String lookup) {
        try {
            redisTemplate.delete(redisKey(lookup));
        } catch (Exception ignored) {
            // Deleting the source document still disables the key on cache expiry or miss fallback.
        }
    }

    private Optional<App> readFromRedis(String lookup) {
        try {
            String value = redisTemplate.opsForValue().get(redisKey(lookup));
            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(value, App.class));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String redisKey(String lookup) {
        return "app-key:" + lookup;
    }
}
