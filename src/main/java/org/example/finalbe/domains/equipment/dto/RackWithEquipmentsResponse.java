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
        // 랙 정보 (간소화)
        RackInfo rack,

        // 장비 목록
        List<EquipmentListResponse> equipments,

        // 통계 정보
        int totalEquipmentCount
) {
    @Builder
    public record RackInfo(
            String rackName,
            Long rackId,
            Long serverRoomId
    ) {
    }

    public static RackWithEquipmentsResponse from(Rack rack, List<EquipmentListResponse> equipments) {
        RackInfo rackInfo = RackInfo.builder()
                .rackName(rack.getRackName())
                .rackId(rack.getId())
                .serverRoomId(rack.getServerRoom() != null ? rack.getServerRoom().getId() : null)
                .build();

        return RackWithEquipmentsResponse.builder()
                .rack(rackInfo)
                .equipments(equipments)
                .totalEquipmentCount(equipments.size())
                .build();
    }
}