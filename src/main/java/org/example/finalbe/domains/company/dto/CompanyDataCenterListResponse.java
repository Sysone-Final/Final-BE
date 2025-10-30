package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

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
    public static CompanyDataCenterListResponse from(DataCenter dataCenter, LocalDateTime grantedAt) {
        if (dataCenter == null) {
            throw new IllegalArgumentException("DataCenter 엔티티가 null입니다.");
        }
        if (grantedAt == null) {
            throw new IllegalArgumentException("grantedAt이 null입니다.");
        }

        return CompanyDataCenterListResponse.builder()
                .dataCenterId(dataCenter.getId())
                .dataCenterCode(dataCenter.getCode())
                .dataCenterName(dataCenter.getName())
                .location(dataCenter.getLocation())
                .managerName(dataCenter.getManager() != null ? dataCenter.getManager().getName() : null)
                .managerPhone(dataCenter.getManager() != null ? dataCenter.getManager().getPhone() : null)
                .grantedAt(grantedAt)
                .build();
    }
}