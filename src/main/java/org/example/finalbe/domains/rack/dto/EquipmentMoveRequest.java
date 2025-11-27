/**
 * 작성자: 황요한
 * 장비 이동 요청 DTO
 */
package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

@Builder
public record EquipmentMoveRequest(
        Integer fromUnit,   // 기존 유닛
        Integer toUnit      // 이동할 유닛
) {
}
