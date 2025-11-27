/**
 * 작성자: 황요한
 * 알림 통계 정보를 제공하는 DTO
 */
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

    // 모든 값이 0인 기본 통계 반환
    public static AlertStatisticsDto empty() {
        return new AlertStatisticsDto(
                0L, 0L,
                0L, 0L,
                0L, 0L, 0L
        );
    }
}
