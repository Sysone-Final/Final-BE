package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.math.BigDecimal;

@Builder
public record DataCenterListResponse(
        Long id,
        String name,
        String code,
        String location,
        String floor,
        DataCenterStatus status,
        Integer maxRackCount,
        Integer currentRackCount,
        Integer availableRackCount,
        BigDecimal totalArea,
        String managerName
) {
    public static DataCenterListResponse from(DataCenter dataCenter) {
        return DataCenterListResponse.builder()
                .id(dataCenter.getId())
                .name(dataCenter.getName())
                .code(dataCenter.getCode())
                .location(dataCenter.getLocation())
                .floor(dataCenter.getFloor())
                .status(dataCenter.getStatus())
                .maxRackCount(dataCenter.getMaxRackCount())
                .currentRackCount(dataCenter.getCurrentRackCount())
                .availableRackCount(dataCenter.getAvailableRackCount())
                .totalArea(dataCenter.getTotalArea())
                .managerName(dataCenter.getManager().getName())
                .build();
    }
}