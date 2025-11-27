/**
 * 작성자: 황요한
 * 장비 배치 요청 DTO
 */
package org.example.finalbe.domains.rack.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record EquipmentPlacementRequest(
        Integer startUnit,        // 시작 유닛
        Integer unitSize,         // 차지하는 유닛 크기
        BigDecimal powerConsumption // 전력 소모량
) {
}
