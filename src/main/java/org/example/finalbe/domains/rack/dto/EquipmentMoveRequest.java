package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

/**
 * 장비 이동 요청 DTO
 */
@Builder
public record EquipmentMoveRequest(
        Integer fromUnit,
        Integer toUnit
) {
}