// src/main/java/org/example/finalbe/domains/companyserverroom/dto/CompanyServerRoomGroupedByDataCenterResponse.java

package org.example.finalbe.domains.companyserverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;

import java.util.List;

/**
 * 데이터센터별로 그룹화된 서버실 응답 DTO
 * 서버실의 행(rows)과 열(columns) 정보 포함
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
            Integer rows,           // 행 정보 추가
            Integer columns,        // 열 정보 추가
            ServerRoomStatus status,
            String description
    ) {}
}