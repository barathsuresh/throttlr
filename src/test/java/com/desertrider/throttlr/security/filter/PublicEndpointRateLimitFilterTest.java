package com.desertrider.throttlr.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.desertrider.throttlr.security.ip.ClientIpResolver;
import com.desertrider.throttlr.security.ratelimit.PublicRateLimitDecision;
import com.desertrider.throttlr.security.ratelimit.PublicRateLimitPolicy;
import com.desertrider.throttlr.security.ratelimit.PublicRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;

class PublicEndpointRateLimitFilterTest {
    @Test
    void doFilterAllowsNonPublicRouteWithoutRateLimitCheck() throws ServletException, IOException {
        RecordingPublicRateLimitService service = new RecordingPublicRateLimitService(PublicRateLimitDecision.allow());
        PublicEndpointRateLimitFilter filter = new PublicEndpointRateLimitFilter(
                service,
                new ClientIpResolver(false),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/apps");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertFalse(service.called);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterBlocksLimitedPublicRouteWithJson429() throws ServletException, IOException {
        RecordingPublicRateLimitService service = new RecordingPublicRateLimitService(
                PublicRateLimitDecision.blocked(Duration.ofSeconds(30)));
        PublicEndpointRateLimitFilter filter = new PublicEndpointRateLimitFilter(
                service,
                new ClientIpResolver(false),
                new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertTrue(service.called);
        assertEquals("login", service.policy.name());
        assertEquals("127.0.0.1", service.clientIp);
        assertEquals(429, response.getStatus());
        assertEquals("30", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Too many requests"));
    }

    private static class RecordingPublicRateLimitService implements PublicRateLimitService {
        private final PublicRateLimitDecision decision;
        private boolean called;
        private PublicRateLimitPolicy policy;
        private String clientIp;

        private RecordingPublicRateLimitService(PublicRateLimitDecision decision) {
            this.decision = decision;
        }

        @Override
        public PublicRateLimitDecision check(PublicRateLimitPolicy policy, String clientIp) {
            this.called = true;
            this.policy = policy;
            this.clientIp = clientIp;
            return decision;
        }
    }
}
