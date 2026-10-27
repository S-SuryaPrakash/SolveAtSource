package com.example.resolveX.dto;

import com.example.resolveX.entity.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Data an administrator supplies when provisioning a new ResolveX user. */
public record CreateUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must be at most 320 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 15, max = 128, message = "Password must be between 15 and 128 characters")
        String password,

        @NotNull(message = "Role is required")
        UserRole role
) {
}
