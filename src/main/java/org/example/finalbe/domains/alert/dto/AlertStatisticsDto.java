package org.example.finalbe.domains.alert.dto;

public record AlertStatisticsDto(
        Long totalAlerts,
        Long triggeredAlerts,
        Long acknowledgedAlerts,
        Long resolvedAlerts,

        Long criticalAlerts,
        Long warningAlerts,

        Long equipmentAlerts,
        Long rackAlerts,
        Long serverRoomAlerts,
        Long dataCenterAlerts
) {
    /**
     * 빌더 패턴 대체 - 모든 필드 0으로 초기화
     */
    public static AlertStatisticsDto empty() {
        return new AlertStatisticsDto(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }
}