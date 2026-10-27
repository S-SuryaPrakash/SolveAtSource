package com.example.resolveX.dto;

import java.time.LocalDateTime;

import com.example.resolveX.entity.UserRole;

/** Safe user representation for API responses; it deliberately excludes passwordHash. */
public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
