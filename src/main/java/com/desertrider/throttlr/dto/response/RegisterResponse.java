package com.desertrider.throttlr.dto.response;

public record RegisterResponse(
        String accountId,
        String passphrase, // shown ONCE, never stored
        String message) {
}