package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DataCenterStatus;

import java.math.BigDecimal;

@Builder
public record DataCenterUpdateRequest(
        String name,
        String code,
        String location,
        String floor,
        Integer rows,
        Integer columns,
        String backgroundImageUrl,
        DataCenterStatus status,
        String description,
        BigDecimal totalArea,
        BigDecimal totalPowerCapacity,
        BigDecimal totalCoolingCapacity,
        Integer maxRackCount,
        BigDecimal temperatureMin,
        BigDecimal temperatureMax,
        BigDecimal humidityMin,
        BigDecimal humidityMax,
        Long managerId
) {
}