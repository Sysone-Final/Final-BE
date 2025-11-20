package org.example.finalbe.domains.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatisticsDto {

    private Long totalAlerts;
    private Long triggeredAlerts;
    private Long acknowledgedAlerts;
    private Long resolvedAlerts;

    private Long criticalAlerts;
    private Long warningAlerts;

    private Long equipmentAlerts;
    private Long rackAlerts;
    private Long serverRoomAlerts;
    private Long dataCenterAlerts;
}