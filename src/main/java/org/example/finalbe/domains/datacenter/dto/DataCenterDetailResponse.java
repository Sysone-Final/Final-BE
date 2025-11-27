/**
 * 작성자: 황요한
 * 데이터센터 상세 조회 응답 DTO
 * - 데이터센터 기본 정보
 * - 소속 서버실 목록 포함
 */
package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.serverroom.dto.ServerRoomSimpleResponse;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DataCenterDetailResponse(
        Long id,                                   // 데이터센터 ID
        String code,                               // 데이터센터 코드
        String name,                               // 이름
        String address,                            // 주소
        String description,                        // 설명
        List<ServerRoomSimpleResponse> serverRooms,// 서버실 목록
        LocalDateTime createdAt,                   // 생성일
        LocalDateTime updatedAt                    // 수정일
) {

    /**
     * Entity → DTO 변환
     * @param dataCenter DataCenter 엔티티
     * @param serverRooms 포함된 서버실 목록
     * @return DTO
     */
    public static DataCenterDetailResponse from(
            DataCenter dataCenter,
            List<ServerRoomSimpleResponse> serverRooms
    ) {
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
