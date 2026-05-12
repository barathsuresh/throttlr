package com.desertrider.throttlr.security.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.desertrider.throttlr.dto.response.ErrorResponse;
import com.desertrider.throttlr.security.ip.ClientIpResolver;
import com.desertrider.throttlr.security.ratelimit.PublicRateLimitDecision;
import com.desertrider.throttlr.security.ratelimit.PublicRateLimitPolicy;
import com.desertrider.throttlr.security.ratelimit.PublicRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PublicEndpointRateLimitFilter extends OncePerRequestFilter {
    private static final Map<String, PublicRateLimitPolicy> POLICIES = Map.of(
            key("POST", "/api/auth/register"), new PublicRateLimitPolicy("register", 5, Duration.ofMinutes(10)),
            key("POST", "/api/auth/login"), new PublicRateLimitPolicy("login", 10, Duration.ofMinutes(1)),
            key("POST", "/api/demo/app-key"), new PublicRateLimitPolicy("demo-app-key", 3, Duration.ofMinutes(10)));

    private final PublicRateLimitService publicRateLimitService;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<PublicRateLimitPolicy> policy = policyFor(request);
        if (policy.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIpResolver.resolve(request);
        PublicRateLimitDecision decision = publicRateLimitService.check(policy.get(), clientIp);
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("[RATE-LIMIT] 429 blocked - endpoint: {}, ip: [{}], retryAfter: {}s",
                policy.get().name(), clientIp, decision.retryAfter().toSeconds());
        writeTooManyRequests(response, decision.retryAfter());
    }

    private Optional<PublicRateLimitPolicy> policyFor(HttpServletRequest request) {
        return Optional.ofNullable(POLICIES.get(key(request.getMethod(), request.getRequestURI())));
    }

    private void writeTooManyRequests(HttpServletResponse response, Duration retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter.toSeconds()));
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse("Too many requests. Please try again later.", 429, System.currentTimeMillis()));
    }

    private static String key(String method, String path) {
        return method + " " + path;
    }
}
