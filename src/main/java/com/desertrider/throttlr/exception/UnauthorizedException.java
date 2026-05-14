package com.desertrider.throttlr.exception;

/**
 * Thrown when authentication fails (invalid/expired JWT) or app key invalid.
 * Maps to 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
