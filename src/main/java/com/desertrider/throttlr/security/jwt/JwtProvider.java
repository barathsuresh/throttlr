package com.desertrider.throttlr.security.jwt;

import java.sql.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.desertrider.throttlr.config.JwtProperties;
import com.desertrider.throttlr.model.Account;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtProvider {
    private final JwtProperties jwtProperties;

    public JwtProperties getJwtProperties() {
        return jwtProperties;
    }

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            log.debug("[JWT] Extracting JWT from Authorization header: {}", tokenPreview(header));
            return header.substring(7);
        }
        return null;
    }

    public String tokenPreview(String token) {
        int previewLength = Math.min(10, token.length());
        return token.substring(0, previewLength) + "...(len=" + token.length() + ")";
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .requireIssuer(jwtProperties.getIssuer())
                    .requireAudience(jwtProperties.getAudience())
                    .build()
                    .parseClaimsJws(authToken);
            log.debug("[JWT] JWT token validation successful");
            return true; // Token is valid
        } catch (MalformedJwtException e) {
            log.warn("[JWT] JWT token validation failed - error: MalformedJwtException, message: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] JWT token validation failed - error: ExpiredJwtException, message: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("[JWT] JWT token validation failed - error: UnsupportedJwtException, message: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("[JWT] JWT token validation failed - error: IllegalArgumentException, message: {}",
                    e.getMessage());
        } catch (Exception e) {
            log.error("[JWT] Unexpected error during JWT validation - error: {}", e.getMessage(), e);
        }
        return false; // Token validation failed
    }

    public String generateToken(Account user, String tenantId) {
        Date now = new Date(System.currentTimeMillis());

        // convert Duration to milliseconds for expiry calculation
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration().toMillis());

        return Jwts.builder()
                // who the token is about — user's id as subject
                .setSubject(user.getId())

                // token metadata
                .setIssuer(jwtProperties.getIssuer())
                .setAudience(jwtProperties.getAudience())
                .setIssuedAt(now)
                .setExpiration(expiry)

                // sign with secret key
                .signWith(getSigningKey())
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}
