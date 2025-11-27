/**
 * 작성자: 황요한
 * 랙 수정 요청 DTO
 */
package org.example.finalbe.domains.rack.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;

import java.math.BigDecimal;

@Builder
public record RackUpdateRequest(
        String rackName,           // 랙 이름
        BigDecimal gridX,          // X 좌표
        BigDecimal gridY,          // Y 좌표
        Integer totalUnits,        // 전체 유닛 수
        DoorDirection doorDirection,   // 문 방향
        ZoneDirection zoneDirection,   // 존 방향
        BigDecimal maxPowerCapacity,   // 최대 전력 용량
        String manufacturer,       // 제조사
        String serialNumber,       // 시리얼 번호
        RackStatus status,         // 랙 상태
        RackType rackType,         // 랙 타입
        String notes               // 비고
) {
}
