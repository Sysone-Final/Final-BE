/**
 * 작성자: 황요한
 * 알림 레벨 Enum (경고 / 위험)
 */
package org.example.finalbe.domains.common.enumdir;

public enum AlertLevel {

    WARNING("경고", 1),
    CRITICAL("위험", 2);

    private final String description;
    private final int severity;

    AlertLevel(String description, int severity) {
        this.description = description;
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public int getSeverity() {
        return severity;
    }

    // 현재 레벨이 다른 레벨보다 심각한지 비교
    public boolean isMoreSevereThan(AlertLevel other) {
        return this.severity > other.severity;
    }
}
