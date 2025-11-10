package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.serverroom.dto.ServerRoomSimpleResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터센터 상세 조회 응답 DTO
 */
@Builder
public record DataCenterDetailResponse(
        Long id,
        String code,
        String name,
        String address,
        String description,
        List<ServerRoomSimpleResponse> serverRooms,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Entity → DTO 변환
     */
    public static DataCenterDetailResponse from(DataCenter dataCenter, List<ServerRoomSimpleResponse> serverRooms) {
        if (dataCenter == null) {
            throw new IllegalArgumentException("DataCenter 엔티티가 null입니다.");
        }

        return DataCenterDetailResponse.builder()
                .id(dataCenter.getId())
                .code(dataCenter.getCode())
                .name(dataCenter.getName())
                .address(dataCenter.getAddress())
                .description(dataCenter.getDescription())
                .serverRooms(serverRooms)
                .createdAt(dataCenter.getCreatedAt())
                .updatedAt(dataCenter.getUpdatedAt())
                .build();
    }
}