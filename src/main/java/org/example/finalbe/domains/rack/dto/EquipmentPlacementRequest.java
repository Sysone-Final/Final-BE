package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * 장비 배치 요청 DTO
 */
@Builder
public record EquipmentPlacementRequest(
        Integer startUnit,
        Integer unitSize,
        BigDecimal powerConsumption
) {
}