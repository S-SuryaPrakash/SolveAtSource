package com.example.resolveX.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidentRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,
        @NotBlank(message = "Status is required")
        @Size(max = 255, message = "Status must be at most 255 characters")
        String status,
        @NotBlank(message = "Priority is required")
        @Size(max = 255, message = "Priority must be at most 255 characters")
        String priority,
        @NotBlank(message = "Category is required")
        @Size(max = 255, message = "Category must be at most 255 characters")
        String category
) {
}
