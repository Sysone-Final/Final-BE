package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

@Builder
public record EquipmentMoveRequest(
        Integer fromUnit,
        Integer toUnit
) {
}
