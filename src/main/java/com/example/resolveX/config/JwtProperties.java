package com.example.resolveX.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** External JWT settings supplied by the deployment environment. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long expirationMinutes
) {
}
