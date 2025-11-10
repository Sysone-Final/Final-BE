package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

/**
 * 데이터센터 목록 조회 응답 DTO
 */
@Builder
public record DataCenterListResponse(
        Long id,
        String code,
        String name
) {
    /**
     * Entity → DTO 변환
     */
    public static DataCenterListResponse from(DataCenter dataCenter) {
        if (dataCenter == null) {
            throw new IllegalArgumentException("DataCenter 엔티티가 null입니다.");
        }

        return DataCenterListResponse.builder()
                .id(dataCenter.getId())
                .code(dataCenter.getCode())
                .name(dataCenter.getName())
                .build();
    }
}