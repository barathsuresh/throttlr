package com.desertrider.throttlr.security.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.desertrider.throttlr.security.filter.JwtAuthenticationFilter;
import com.desertrider.throttlr.security.filter.PublicEndpointRateLimitFilter;
import com.desertrider.throttlr.security.ip.ClientIpResolver;
import com.desertrider.throttlr.security.jwt.JwtAuthenticationEntryPoint;
import com.desertrider.throttlr.security.jwt.JwtProvider;
import com.desertrider.throttlr.security.ratelimit.PublicRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Central Security configuration class that defines the HTTP security settings
 */
@Configuration
public class SecurityConfiguration {
    /**
     * Defines the JWT authentication filter bean that will be used to validate JWT tokens in incoming requests.
     * @param jwtProvider
     * @return
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtProvider jwtProvider) {
        return new JwtAuthenticationFilter(jwtProvider);
    }

    /**
     * Defines the public endpoint rate limit filter bean that will be used to limit requests to public endpoints.
     * @param publicRateLimitService
     * @param clientIpResolver
     * @param objectMapper
     * @return
     */
    @Bean
    public PublicEndpointRateLimitFilter publicEndpointRateLimitFilter(
            PublicRateLimitService publicRateLimitService,
            ClientIpResolver clientIpResolver,
            ObjectMapper objectMapper) {
        return new PublicEndpointRateLimitFilter(publicRateLimitService, clientIpResolver, objectMapper);
    }

    /**
     * Defines the security filter chain that configures CORS, CSRF, session management, exception handling, and request authorization.
     * @param http
     * @param jwtAuthenticationFilter
     * @param jwtAuthenticationEntryPoint
     * @param publicEndpointRateLimitFilter
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            PublicEndpointRateLimitFilter publicEndpointRateLimitFilter)
            throws Exception {
        return http
                .cors(cors -> {
                }) // Spring Pullus it. from the configured CorsConfigurationSource bean
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/check",
                                "/api/demo/app-key",
                                "/api/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(publicEndpointRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * Defines the CORS configuration source bean that configures allowed origins, methods, headers, and exposed headers for CORS requests.
     * @param environment
     * @return
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(Environment environment) {
        String origins = environment.getProperty(
                "app.security.cors.allowed-origins",
                "http://localhost:5173,http://127.0.0.1:5173");

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(splitCsv(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-App-Key"));
        configuration.setExposedHeaders(List.of("Retry-After"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
