package com.desertrider.throttlr.security.filter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.desertrider.throttlr.security.jwt.JwtProvider;
import com.desertrider.throttlr.security.model.AccountPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter that intercepts incoming HTTP requests and checks for the presence of
 * a JWT token in the Authorization header.
 * If a valid token is found, it extracts the user information and sets the
 * authentication in the SecurityContext for the request.
 */
/**
 * Per-request filter: extracts JWT from header, validates it, sets
 * authentication in SecurityContext.
 * Runs once per HTTP request.
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    /**
     * Extracts JWT from header, validates token, and sets SecurityContext if valid.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = jwtProvider.getJwtFromHeader(request);
        if (token != null && jwtProvider.validateJwtToken(token)) {
            String accountId = jwtProvider.getUserIdFromToken(token);
            log.debug("[JWT] Token authenticated - accountId: [{}]", accountId);
            AccountPrincipal principal = new AccountPrincipal(accountId);
            // No credentials or authorities needed for stateless JWT auth
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.emptyList());
            // Set in ThreadLocal SecurityContext for downstream access
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

}
