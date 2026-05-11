package com.desertrider.throttlr.security.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityConfigurationTest {
    @Test
    void corsConfigurationAllowsConfiguredFrontendOrigins() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.security.cors.allowed-origins", "http://localhost:5173,http://127.0.0.1:5173");
        SecurityConfiguration configuration = new SecurityConfiguration();
        CorsConfigurationSource source = configuration.corsConfigurationSource(environment);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/demo/app-key");

        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertEquals(List.of("http://localhost:5173", "http://127.0.0.1:5173"), cors.getAllowedOrigins());
        assertTrue(cors.getAllowedMethods().contains("POST"));
        assertTrue(cors.getAllowedHeaders().contains("Authorization"));
        assertTrue(cors.getAllowedHeaders().contains("X-App-Key"));
    }
}
