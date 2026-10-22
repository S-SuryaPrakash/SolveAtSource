package com.example.resolveX.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.example.resolveX.entity.IncidentPriority;
import com.example.resolveX.entity.IncidentStatus;

public record IncidentRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must be at most 120 characters")
        String title,
        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,
        IncidentStatus status,
        IncidentPriority priority,
        @NotBlank(message = "Category is required")
        @Size(max = 255, message = "Category must be at most 255 characters")
        String category
) {
}
