package com.example.resolveX.dto;

public record IncidentResponse(
        Long id,
        String description,
        String priority,
        String category
) {
}
