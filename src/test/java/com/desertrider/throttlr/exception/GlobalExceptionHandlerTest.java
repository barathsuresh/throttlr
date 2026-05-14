package com.desertrider.throttlr.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.ResponseEntity;

import com.desertrider.throttlr.dto.response.ErrorResponse;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {
    @Test
    void genericExceptionDoesNotExposeInternalMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
                new RuntimeException("could not connect to mongodb://secret-host"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal server error", response.getBody().message());
    }

    @Test
    void redisCommandLimitReturnsSpecificServiceUnavailableMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleRedisSystemException(
                new RedisSystemException("ERR max monthly command limit reached", new RuntimeException()));

        assertEquals(503, response.getStatusCode().value());
        assertEquals("Redis command limit reached. Please upgrade the Redis plan or wait for quota reset.",
                response.getBody().message());
    }

    @Test
    void redisConnectionFailureDoesNotExposeInternalMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> response = handler.handleRedisConnectionFailure(
                new RedisConnectionFailureException("redis://secret-host connection refused"));

        assertEquals(503, response.getStatusCode().value());
        assertEquals("Redis is temporarily unavailable", response.getBody().message());
    }
}
