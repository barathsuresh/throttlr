package com.desertrider.throttlr.security.ip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
    @Test
    void resolveIgnoresForwardedForWhenHeadersAreNotTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void resolveUsesFirstForwardedForIpWhenHeadersAreTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");

        assertEquals("203.0.113.10", resolver.resolve(request));
    }

    @Test
    void resolveFallsBackToRemoteAddressWhenTrustedHeaderIsBlank() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "   ");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }
}
