package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.math.BigDecimal;

/**
 * 전산실 목록 조회 응답 DTO
 */
@Builder
public record DataCenterListResponse(
        Long id,
        String name,
        String code,
        String location,
        Integer floor,
        DataCenterStatus status,
        Integer maxRackCount,
        Integer currentRackCount,
        Integer availableRackCount,
        BigDecimal totalArea,
        String managerName,
        Long companyId,        // ★ 추가
        String companyName     // ★ 추가
) {
    /**
     * Entity → DTO 변환
     */
    public static DataCenterListResponse from(DataCenter dataCenter) {
        return DataCenterListResponse.builder()
                .id(dataCenter.getId())
                .name(dataCenter.getName())
                .code(dataCenter.getCode())
                .location(dataCenter.getLocation())
                .floor(dataCenter.getFloor())
                .status(dataCenter.getStatus())
                .maxRackCount(dataCenter.getMaxRackCount())
                .currentRackCount(dataCenter.getCurrentRackCount())
                .availableRackCount(dataCenter.getAvailableRackCount())
                .totalArea(dataCenter.getTotalArea())
                .managerName(dataCenter.getManager().getName())
                .companyId(dataCenter.getCompany().getId())           // ★ 추가
                .companyName(dataCenter.getCompany().getName())       // ★ 추가
                .build();
    }
}