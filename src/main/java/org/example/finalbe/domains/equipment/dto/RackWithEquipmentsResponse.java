// 작성자: 황요한
// 설명: 랙 정보와 해당 랙에 포함된 장비 목록을 함께 제공하는 응답 DTO

package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;
import org.example.finalbe.domains.rack.domain.Rack;

import java.util.List;

@Builder
public record RackWithEquipmentsResponse(
        RackInfo rack,
        List<EquipmentListResponse> equipments,
        int totalEquipmentCount
) {

    /**
     * 랙 정보만 간단히 담는 DTO
     */
    @Builder
    public record RackInfo(
            String rackName,
            Long rackId,
            Long serverRoomId
    ) {
    }

    /**
     * Rack 엔티티와 장비 목록을 받아 응답 DTO로 변환
     */
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
