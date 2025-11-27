/**
 * 작성자: 황요한
 * 알림 대상 타입 열거형
 */
package org.example.finalbe.domains.common.enumdir;

public enum TargetType {
    EQUIPMENT("장비"),
    RACK("랙"),
    SERVER_ROOM("서버실"),
    DATA_CENTER("데이터센터");

    private final String description;

    TargetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}