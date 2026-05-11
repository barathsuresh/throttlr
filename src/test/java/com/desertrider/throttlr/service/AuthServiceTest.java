package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.desertrider.throttlr.dto.request.LoginRequest;

class AuthServiceTest {
    @Test
    void loginRejectsOverlongPassphrase() {
        AuthService authService = new AuthService(null, null, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("p".repeat(301))));
    }
}
