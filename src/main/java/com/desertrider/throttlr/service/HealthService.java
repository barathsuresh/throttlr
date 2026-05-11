package com.desertrider.throttlr.service;

import org.bson.Document;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import com.desertrider.throttlr.dto.response.HealthResponse;

@Service
public class HealthService {
    private final RedisConnectionFactory redisConnectionFactory;
    private final MongoDatabaseFactory mongoDatabaseFactory;

    public HealthService(RedisConnectionFactory redisConnectionFactory, MongoDatabaseFactory mongoDatabaseFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.mongoDatabaseFactory = mongoDatabaseFactory;
    }

    public HealthResponse health() {
        return new HealthResponse("throttlr", "UP", "Application is running");
    }

    public HealthResponse redisHealth() {
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            String pong = connection.ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                return new HealthResponse("redis", "UP", "Redis responded to ping");
            }

            return new HealthResponse("redis", "DOWN", "Redis ping returned: " + pong);
        } catch (Exception e) {
            return new HealthResponse("redis", "DOWN", "Redis is unavailable");
        }
    }

    public HealthResponse mongoHealth() {
        try {
            Document result = mongoDatabaseFactory.getMongoDatabase().runCommand(new Document("ping", 1));
            Number ok = result.get("ok", Number.class);
            if (ok != null && ok.doubleValue() == 1.0) {
                return new HealthResponse("mongo", "UP", "MongoDB responded to ping");
            }

            return new HealthResponse("mongo", "DOWN", "MongoDB ping returned unexpected response");
        } catch (Exception e) {
            return new HealthResponse("mongo", "DOWN", "MongoDB is unavailable");
        }
    }
}
