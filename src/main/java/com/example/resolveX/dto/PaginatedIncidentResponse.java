package com.example.resolveX.dto;

import java.util.List;

public record PaginatedIncidentResponse(
        List<IncidentResponse> items,
        int page,
        int size,
        int totalItems,
        boolean hasNext
) {
}
