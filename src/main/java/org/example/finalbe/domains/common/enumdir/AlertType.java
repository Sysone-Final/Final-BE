package org.example.finalbe.domains.common.enumdir;

/**
 * 알람 타입 Enum
 */
public enum AlertType {
    CPU("CPU", "CPU 사용률 초과"),
    MEMORY("메모리", "메모리 사용률 초과"),
    DISK("디스크", "디스크 사용률 초과"),
    NETWORK("네트워크", "네트워크 이상"),
    TEMPERATURE("온도", "온도 임계값 초과"),
    HUMIDITY("습도", "습도 임계값 초과"),
    SYSTEM("시스템", "시스템 이상");

    private final String displayName;
    private final String description;

    AlertType(String displayName, String description) {
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