package org.example.finalbe.domains.companyserverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.companyserverroom.domain.CompanyServerRoom;

import java.time.LocalDateTime;

/**
 * 회사-전산실 매핑 응답 DTO
 */
@Builder
public record CompanyServerRoomResponse(
        Long id,
        Long companyId,
        String location,
        String code,
        String companyName,
        Long serverRoomId,
        String dataCenterName,
        String description,
        String grantedBy,
        LocalDateTime createdAt
) {
    /**
     * Entity → DTO 변환
     */
    public static CompanyServerRoomResponse from(CompanyServerRoom companyServerRoom) {
        return CompanyServerRoomResponse.builder()
                .id(companyServerRoom.getId())
                .companyId(companyServerRoom.getCompany().getId())
                .location(companyServerRoom.getServerRoom().getLocation())
                .code(companyServerRoom.getServerRoom().getCode())
                .companyName(companyServerRoom.getCompany().getName())
                .serverRoomId(companyServerRoom.getServerRoom().getId())
                .dataCenterName(companyServerRoom.getServerRoom().getName())
                .description(companyServerRoom.getDescription())
                .grantedBy(companyServerRoom.getGrantedBy())
                .createdAt(companyServerRoom.getCreatedAt())
                .build();
    }
}