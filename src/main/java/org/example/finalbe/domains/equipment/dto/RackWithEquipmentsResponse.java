package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.util.List;

/**
 * 랙 정보 + 장비 목록 통합 응답 DTO
 */
@Builder
public record RackWithEquipmentsResponse(
        // 랙 정보
        Long rackId,
        String rackName,
        String rackCode,
        Integer totalUnits,
        Integer usedUnits,
        Integer availableUnits,
        BigDecimal usageRate,
        String status,
        Long serverRoomId,
        String serverRoomName,

        // 장비 목록
        List<EquipmentListResponse> equipments,

        // 통계 정보
        int totalEquipmentCount
) {
    public static RackWithEquipmentsResponse from(Rack rack, List<EquipmentListResponse> equipments) {
        return RackWithEquipmentsResponse.builder()
                .rackId(rack.getId())
                .rackName(rack.getRackName())
                .totalUnits(rack.getTotalUnits())
                .usedUnits(rack.getUsedUnits())
                .availableUnits(rack.getAvailableUnits())
                .usageRate(rack.getUsageRate())
                .status(rack.getStatus() != null ? rack.getStatus().name() : null)
                .serverRoomId(rack.getServerRoom() != null ? rack.getServerRoom().getId() : null)
                .serverRoomName(rack.getServerRoom() != null ? rack.getServerRoom().getName() : null)
                .equipments(equipments)
                .totalEquipmentCount(equipments.size())
                .build();
    }
}