package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

@Builder
public record EquipmentPlacementRequest(
        Integer startUnit,
        Integer unitSize,
        Double powerConsumption,
        Double weight
) {
}