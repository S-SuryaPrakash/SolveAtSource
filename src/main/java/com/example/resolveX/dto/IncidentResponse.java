package com.example.resolveX.dto;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        String category
) {
    public IncidentResponse(Long id, String description, String priority, String category) {
        this(id, null, description, null, priority, category);
    }
}
