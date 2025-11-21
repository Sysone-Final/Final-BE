package org.example.finalbe.domains.common.enumdir;

/**
 * 알림 심각도 Enum - 장비 상태와 통일
 */
public enum AlertSeverity {
    NORMAL("정상", "정상 상태", 0),
    WARNING("경고", "임계값 초과 경고", 1),
    ERROR("오류", "심각한 오류 발생", 2);

    private final String displayName;
    private final String description;
    private final int level;

    AlertSeverity(String displayName, String description, int level) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    public boolean isMoreSevereThan(AlertSeverity other) {
        return this.level > other.level;
    }

    /**
     * EquipmentStatus와 매핑
     */
    public static AlertSeverity fromEquipmentStatus(EquipmentStatus status) {
        return switch (status) {
            case NORMAL -> NORMAL;
            case WARNING -> WARNING;
            case ERROR -> ERROR;
            default -> NORMAL;
        };
    }

    /**
     * AlertLevel에서 변환
     */
    public static AlertSeverity fromAlertLevel(AlertLevel level) {
        return switch (level) {
            case WARNING -> WARNING;
            case CRITICAL -> ERROR;
        };
    }
}