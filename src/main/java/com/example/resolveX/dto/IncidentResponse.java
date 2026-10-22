package com.example.resolveX.dto;

import java.time.LocalDateTime;

import com.example.resolveX.entity.IncidentPriority;
import com.example.resolveX.entity.IncidentStatus;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        IncidentStatus status,
        IncidentPriority priority,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public IncidentResponse(Long id, String description, IncidentPriority priority, String category) {
        this(id, null, description, null, priority, category, null, null);
    }

    public IncidentResponse(Long id, String title, String description, IncidentStatus status, IncidentPriority priority, String category) {
        this(id, title, description, status, priority, category, null, null);
    }
}
