package com.example.resolveX.dto;

import com.example.resolveX.entity.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Safe account fields an administrator may update for an existing ResolveX user. */
public record UpdateUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotNull(message = "Role is required")
        UserRole role,

        @NotNull(message = "Enabled state is required")
        Boolean enabled
) {
}
