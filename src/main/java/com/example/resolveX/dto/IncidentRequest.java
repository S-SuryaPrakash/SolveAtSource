package com.example.resolveX.dto;

public record IncidentRequest(
        String title,
        String description,
        String status,
        String priority,
        String category
) {
}
