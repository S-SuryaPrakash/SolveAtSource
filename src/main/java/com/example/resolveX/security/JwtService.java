package com.example.resolveX.security;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.example.resolveX.config.JwtProperties;
import com.example.resolveX.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/** Creates and validates ResolveX HS256 access tokens. */
@Service
public class JwtService {

    public static final String EMAIL_CLAIM = "email";
    public static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateAccessToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("A persisted user is required to generate an access token");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(accessTokenExpiresInSeconds());
        return Jwts.builder()
                .issuer(requiredIssuer())
                .subject(user.getId().toString())
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(requiredIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long accessTokenExpiresInSeconds() {
        if (properties.expirationMinutes() <= 0) {
            throw new IllegalStateException("JWT expiration must be greater than zero");
        }
        return Math.multiplyExact(properties.expirationMinutes(), 60L);
    }

    private SecretKey signingKey() {
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must be Base64 encoded", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String requiredIssuer() {
        String issuer = properties.issuer();
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("JWT issuer must be configured");
        }
        return issuer;
    }
}
