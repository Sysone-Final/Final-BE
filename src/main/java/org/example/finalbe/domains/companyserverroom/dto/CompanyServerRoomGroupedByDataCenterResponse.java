package org.example.finalbe.domains.companyserverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;

import java.util.List;

/**
 * 데이터센터별로 그룹화된 서버실 응답 DTO
 */
@Builder
public record CompanyServerRoomGroupedByDataCenterResponse(
        Long dataCenterId,
        String dataCenterName,
        String dataCenterCode,
        String dataCenterAddress,
        List<ServerRoomInfo> serverRooms
) {
    @Builder
    public record ServerRoomInfo(
            Long id,
            String name,
            String code,
            String location,
            Integer floor,
            ServerRoomStatus status
    ) {}
}