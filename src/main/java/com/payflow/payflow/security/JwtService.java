package com.payflow.payflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${payflow.jwt.secret}")
    private String secret;

    @Value("${payflow.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${payflow.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(UUID userId, String email) {
        return buildToken(userId, email, accessTokenExpirationMs, "ACCESS");
    }

    public String generateRefreshToken(UUID userId, String email) {
        return buildToken(userId, email, refreshTokenExpirationMs, "REFRESH");
    }

    private String buildToken(UUID userId, String email, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public boolean isValidToken(String token, UUID expectedUserId) {
        UUID userId = extractUserId(token);
        return userId.equals(expectedUserId) && !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}