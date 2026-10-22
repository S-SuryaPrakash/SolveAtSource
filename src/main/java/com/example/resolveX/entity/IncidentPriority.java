package com.example.resolveX.entity;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IncidentPriority {
    P4_LOW("P4 - Low"),
    P3_MEDIUM("P3 - Medium"),
    P2_HIGH("P2 - High"),
    P1_CRITICAL("P1 - Critical");

    private final String displayValue;

    IncidentPriority(String displayValue) {
        this.displayValue = displayValue;
    }

    @JsonCreator
    public static IncidentPriority fromValue(String value) {
        return Arrays.stream(values())
                .filter(priority -> priority.name().equalsIgnoreCase(value)
                        || priority.displayValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "priority must be one of: P4 - Low, P3 - Medium, P2 - High, P1 - Critical"));
    }

    @JsonValue
    public String getDisplayValue() {
        return displayValue;
    }
}
