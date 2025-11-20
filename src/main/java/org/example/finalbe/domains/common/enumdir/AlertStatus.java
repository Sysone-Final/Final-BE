package org.example.finalbe.domains.common.enumdir;

public enum AlertStatus {
    TRIGGERED("발생", "알림이 발생했습니다"),
    ACKNOWLEDGED("확인됨", "관리자가 확인했습니다"),
    RESOLVED("해결됨", "문제가 해결되었습니다");

    private final String description;
    private final String message;

    AlertStatus(String description, String message) {
        this.description = description;
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public String getMessage() {
        return message;
    }
}