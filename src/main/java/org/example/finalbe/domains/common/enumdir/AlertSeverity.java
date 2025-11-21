package org.example.finalbe.domains.common.enumdir;

/**
 * 알람 심각도 Enum
 */
public enum AlertSeverity {
    WARNING("경고", "임계값 초과 경고"),
    CRITICAL("심각", "임계값 심각 수준 초과");

    private final String displayName;
    private final String description;

    AlertSeverity(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}