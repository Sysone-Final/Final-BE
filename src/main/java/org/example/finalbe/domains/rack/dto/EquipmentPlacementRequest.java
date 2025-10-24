package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record EquipmentPlacementRequest(
        Integer startUnit,
        Integer unitSize,
        BigDecimal powerConsumption,
        BigDecimal weight
) {
}