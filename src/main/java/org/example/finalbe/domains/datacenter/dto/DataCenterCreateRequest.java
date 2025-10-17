package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.member.domain.Member;

import java.math.BigDecimal;

@Builder
public record DataCenterCreateRequest(
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
    public DataCenter toEntity(Member manager, String createdBy) {
        return DataCenter.builder()
                .name(this.name)
                .code(this.code)
                .location(this.location)
                .floor(this.floor)
                .rows(this.rows)
                .columns(this.columns)
                .backgroundImageUrl(this.backgroundImageUrl)
                .status(this.status != null ? this.status : DataCenterStatus.ACTIVE)
                .description(this.description)
                .totalArea(this.totalArea)
                .totalPowerCapacity(this.totalPowerCapacity)
                .totalCoolingCapacity(this.totalCoolingCapacity)
                .maxRackCount(this.maxRackCount)
                .currentRackCount(0)
                .temperatureMin(this.temperatureMin)
                .temperatureMax(this.temperatureMax)
                .humidityMin(this.humidityMin)
                .humidityMax(this.humidityMax)
                .manager(manager)
                .createdBy(createdBy)
                .build();
    }
}