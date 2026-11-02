package com.railguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service orchestrating JSON Web Token (JWT) generation, validation, and parsing.
 * Enforces startup validation of the JWT secret.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long jwtExpirationMs = 28800000; // 8 hours (reasonable development expiration)

    public JwtService(@Value("${app.jwt.secret:}") String secretEnv) {
        if (secretEnv == null || secretEnv.trim().isEmpty() || secretEnv.equals("change-this-secret-in-production") || secretEnv.equals("replace-with-a-long-random-secret")) {
            throw new IllegalStateException(
                "CRITICAL ERROR: The JWT_SECRET environment variable is missing, blank, or using an unsafe default placeholder. " +
                "The application must fail to start for security preservation."
            );
        }
        
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretEnv.trim());
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException(
                    "JWT_SECRET bytes length is " + keyBytes.length + ". Secret must be at least 256 bits (32 bytes)."
                );
            }
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new IllegalStateException(
                "CRITICAL ERROR: Failed to initialize cryptographic key from JWT_SECRET. " +
                "Ensure it is a valid Base64 encoded string of at least 256 bits. Error: " + e.getMessage(), e
            );
        }
    }

    /**
     * Extracts username (subject) from JWT.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts role claim from JWT.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extracts userId claim from JWT.
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a new JWT access token with user details.
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validates if the token belongs to the given user and has not expired.
     */
    public boolean isTokenValid(String token, String userDetailsUsername) {
        final String username = extractUsername(token);
        return (username.equals(userDetailsUsername)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
