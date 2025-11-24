// src/main/java/org/example/finalbe/domains/alert/dto/AlertStatisticsDto.java
package org.example.finalbe.domains.alert.dto;

public record AlertStatisticsDto(
        Long totalAlerts,
        Long triggeredAlerts,

        Long criticalAlerts,
        Long warningAlerts,

        Long equipmentAlerts,
        Long rackAlerts,
        Long serverRoomAlerts
) {
    /**
     * 빈 통계 (모든 필드 0으로 초기화)
     */
    public static AlertStatisticsDto empty() {
        return new AlertStatisticsDto(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }
}