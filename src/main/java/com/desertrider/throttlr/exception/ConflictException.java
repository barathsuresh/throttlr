package com.desertrider.throttlr.exception;

/**
 * Thrown when resource already exists (e.g., duplicate rule for same client).
 * Maps to 409 Conflict.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}