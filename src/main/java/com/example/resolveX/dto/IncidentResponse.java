package com.example.resolveX.dto;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public IncidentResponse(Long id, String description, String priority, String category) {
        this(id, null, description, null, priority, category, null, null);
    }

    public IncidentResponse(Long id, String title, String description, String status, String priority, String category) {
        this(id, title, description, status, priority, category, null, null);
    }
}
