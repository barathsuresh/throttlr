package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.mongodb.client.MongoDatabase;
import com.desertrider.throttlr.dto.response.HealthResponse;

class HealthServiceTest {
    @Test
    void healthReturnsUpWhenApplicationIsAlive() {
        HealthService service = new HealthService(null, null);

        HealthResponse response = service.health();

        assertEquals("UP", response.status());
        assertEquals("throttlr", response.component());
    }

    @Test
    void redisHealthUsesPing() {
        RedisConnectionFactory redisConnectionFactory = redisConnectionFactory("PONG");
        HealthService service = new HealthService(redisConnectionFactory, null);

        HealthResponse response = service.redisHealth();

        assertEquals("UP", response.status());
        assertEquals("redis", response.component());
    }

    @Test
    void mongoHealthUsesPingCommand() {
        MongoDatabaseFactory mongoDatabaseFactory = mongoDatabaseFactory();
        HealthService service = new HealthService(null, mongoDatabaseFactory);

        HealthResponse response = service.mongoHealth();

        assertEquals("UP", response.status());
        assertEquals("mongo", response.component());
    }

    private RedisConnectionFactory redisConnectionFactory(String pingResponse) {
        RedisConnection redisConnection = (RedisConnection) Proxy.newProxyInstance(
                RedisConnection.class.getClassLoader(),
                new Class<?>[] { RedisConnection.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "ping" -> pingResponse;
                    case "close" -> null;
                    case "toString" -> "RecordingRedisConnection";
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        return (RedisConnectionFactory) Proxy.newProxyInstance(
                RedisConnectionFactory.class.getClassLoader(),
                new Class<?>[] { RedisConnectionFactory.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getConnection" -> redisConnection;
                    case "getReactiveConnection" -> throw new UnsupportedOperationException(method.getName());
                    case "toString" -> "RecordingRedisConnectionFactory";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private MongoDatabaseFactory mongoDatabaseFactory() {
        MongoDatabase mongoDatabase = (MongoDatabase) Proxy.newProxyInstance(
                MongoDatabase.class.getClassLoader(),
                new Class<?>[] { MongoDatabase.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "runCommand" -> new Document("ok", 1);
                    case "toString" -> "RecordingMongoDatabase";
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        return (MongoDatabaseFactory) Proxy.newProxyInstance(
                MongoDatabaseFactory.class.getClassLoader(),
                new Class<?>[] { MongoDatabaseFactory.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMongoDatabase" -> mongoDatabase;
                    case "toString" -> "RecordingMongoDatabaseFactory";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
