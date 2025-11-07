package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 전산실 상세 조회 응답 DTO
 */
@Builder
public record DataCenterDetailResponse(
        Long id,
        String name,
        String code,
        String location,
        Integer floor,
        Integer rows,
        Integer columns,
        DataCenterStatus status,
        String description,
        BigDecimal totalArea,
        BigDecimal totalPowerCapacity,
        BigDecimal totalCoolingCapacity,
        Integer currentRackCount,
        BigDecimal temperatureMin,
        BigDecimal temperatureMax,
        BigDecimal humidityMin,
        BigDecimal humidityMax,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Entity → DTO 변환
     */
    public static DataCenterDetailResponse from(DataCenter dataCenter) {
        return DataCenterDetailResponse.builder()
                .id(dataCenter.getId())
                .name(dataCenter.getName())
                .code(dataCenter.getCode())
                .location(dataCenter.getLocation())
                .floor(dataCenter.getFloor())
                .rows(dataCenter.getRows())
                .columns(dataCenter.getColumns())
                .status(dataCenter.getStatus())
                .description(dataCenter.getDescription())
                .totalArea(dataCenter.getTotalArea())
                .totalPowerCapacity(dataCenter.getTotalPowerCapacity())
                .totalCoolingCapacity(dataCenter.getTotalCoolingCapacity())
                .currentRackCount(dataCenter.getCurrentRackCount())
                .temperatureMin(dataCenter.getTemperatureMin())
                .temperatureMax(dataCenter.getTemperatureMax())
                .humidityMin(dataCenter.getHumidityMin())
                .humidityMax(dataCenter.getHumidityMax())
                .createdAt(dataCenter.getCreatedAt())
                .updatedAt(dataCenter.getUpdatedAt())
                .build();
    }
}