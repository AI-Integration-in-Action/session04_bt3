package com.logistics.incident.enums;

public enum UrgencyLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static UrgencyLevel parseOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return UrgencyLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
