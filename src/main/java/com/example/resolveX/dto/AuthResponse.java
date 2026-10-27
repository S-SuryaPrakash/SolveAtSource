package com.example.resolveX.dto;

/** Safe authentication result returned after a successful login. */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
}
