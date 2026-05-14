package com.desertrider.throttlr.exception;

/**
 * Thrown when requested resource doesn't exist (app, rule, account not found).
 * Maps to 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}