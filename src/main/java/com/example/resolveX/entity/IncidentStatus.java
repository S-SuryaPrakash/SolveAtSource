package com.example.resolveX.entity;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IncidentStatus {
    NEW("New"),
    WORK_IN_PROGRESS("Work In Progress"),
    SOLUTION_PROPOSED("Solution Proposed"),
    CLOSED("Closed");

    private final String displayValue;

    IncidentStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    @JsonCreator
    public static IncidentStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value)
                        || status.displayValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "status must be one of: New, Work In Progress, Solution Proposed, Closed"));
    }

    @JsonValue
    public String getDisplayValue() {
        return displayValue;
    }
}
