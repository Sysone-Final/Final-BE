package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.time.LocalDateTime;

/**
 * 회사의 전산실 목록 조회 응답 DTO
 */
@Builder
public record CompanyDataCenterListResponse(
        Long dataCenterId,
        String dataCenterCode,
        String dataCenterName,
        String location,
        String managerName,
        String managerPhone,
        LocalDateTime grantedAt
) {
    /**
     * DataCenter와 매핑 시간으로 DTO 생성
     */
    public static CompanyDataCenterListResponse from(ServerRoom serverRoom, LocalDateTime grantedAt) {
        if (serverRoom == null) {
            throw new IllegalArgumentException("DataCenter 엔티티가 null입니다.");
        }
        if (grantedAt == null) {
            throw new IllegalArgumentException("grantedAt이 null입니다.");
        }

        return CompanyDataCenterListResponse.builder()
                .dataCenterId(serverRoom.getId())
                .dataCenterCode(serverRoom.getCode())
                .dataCenterName(serverRoom.getName())
                .location(serverRoom.getLocation())
                .grantedAt(grantedAt)
                .build();
    }
}