package com.desertrider.throttlr.model.enums;

/**
 * Rate limiting algorithms supported by Throttlr.
 * TOKEN_BUCKET: Allows burst traffic up to bucket size, refills over time.
 * FIXED_WINDOW: Resets counter on time window boundary (e.g., per hour).
 * SLIDING_WINDOW: Tracks requests in sliding time window (most accurate, higher
 * overhead).
 */
public enum Algorithm {
    TOKEN_BUCKET,
    FIXED_WINDOW,
    SLIDING_WINDOW,
}
