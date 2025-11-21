package org.example.finalbe.domains.common.enumdir;

/**
 * 알림 상태 Enum
 */
public enum AlertStatus {
    TRIGGERED("발생", "알림이 발생됨"),
    ACKNOWLEDGED("확인됨", "알림이 확인됨"),
    RESOLVED("해결됨", "알림이 해결됨");

    private final String displayName;
    private final String description;

    AlertStatus(String displayName, String description) {
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