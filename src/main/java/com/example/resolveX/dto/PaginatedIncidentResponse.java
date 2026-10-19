package com.example.resolveX.dto;

import java.util.List;

public record PaginatedIncidentResponse(
        List<IncidentResponse> items,
        int page,
        int size,
        long totalItems,
        boolean hasNext,
        List<IncidentGroupResponse> groups
) {
    public PaginatedIncidentResponse(List<IncidentResponse> items, int page, int size, long totalItems,
            boolean hasNext) {
        this(items, page, size, totalItems, hasNext, null);
    }
}
