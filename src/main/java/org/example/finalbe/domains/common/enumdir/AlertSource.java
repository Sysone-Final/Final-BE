package org.example.finalbe.domains.common.enumdir;

/**
 * 알림 발생 소스 타입
 */
public enum AlertSource {
    /**
     * 시스템 자동 알림 (임계치 기반)
     * - CPU, Memory, Disk, Network 등의 메트릭이 임계값 초과 시 자동 발생
     */
    SYSTEM("시스템", "임계치 기반 자동 알림"),

    /**
     * 사용자 수동 알림
     * - 사용자가 직접 생성/수정한 알림
     * - 점검, 작업 예정, 커스텀 알림 등
     */
    MANUAL("수동", "사용자 생성 알림");

    private final String description;
    private final String detail;

    AlertSource(String description, String detail) {
        this.description = description;
        this.detail = detail;
    }

    public String getDescription() {
        return description;
    }

    public String getDetail() {
        return detail;
    }
}