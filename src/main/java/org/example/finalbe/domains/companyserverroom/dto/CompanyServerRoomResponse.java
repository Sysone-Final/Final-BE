// src/main/java/org/example/finalbe/domains/companyserverroom/dto/CompanyServerRoomResponse.java

package org.example.finalbe.domains.companyserverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.companyserverroom.domain.CompanyServerRoom;

import java.time.LocalDateTime;

/**
 * 회사-서버실 매핑 응답 DTO
 * 서버실의 행(rows)과 열(columns) 정보 포함
 */
@Builder
public record CompanyServerRoomResponse(
        Long id,
        Long companyId,
        String location,
        String code,
        String companyName,
        Long serverRoomId,
        String serverRoomName,
        Integer rows,        // 서버실 행 추가
        Integer columns,     // 서버실 열 추가
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
                .serverRoomName(companyServerRoom.getServerRoom().getName())
                .rows(companyServerRoom.getServerRoom().getRows())           // 행 정보 추가
                .columns(companyServerRoom.getServerRoom().getColumns())     // 열 정보 추가
                .description(companyServerRoom.getServerRoom().getDescription())
                .grantedBy(companyServerRoom.getGrantedBy())
                .createdAt(companyServerRoom.getCreatedAt())
                .build();
    }
}