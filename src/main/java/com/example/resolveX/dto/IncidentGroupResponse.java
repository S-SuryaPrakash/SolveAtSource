package com.example.resolveX.dto;

import java.util.List;

/** A group of incidents from the current result page. */
public record IncidentGroupResponse(
        String value,
        List<IncidentResponse> items
) {
}
