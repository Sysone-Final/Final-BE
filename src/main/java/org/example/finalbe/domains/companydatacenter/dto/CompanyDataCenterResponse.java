package org.example.finalbe.domains.companydatacenter.dto;

import lombok.Builder;
import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;

import java.time.LocalDateTime;

/**
 * 회사-전산실 매핑 응답 DTO
 */
@Builder
public record CompanyDataCenterResponse(
        Long id,
        Long companyId,
        String companyName,
        Long dataCenterId,
        String dataCenterName,
        String description,
        String grantedBy,
        LocalDateTime createdAt
) {
    /**
     * Entity → DTO 변환
     */
    public static CompanyDataCenterResponse from(CompanyDataCenter companyDataCenter) {
        return CompanyDataCenterResponse.builder()
                .id(companyDataCenter.getId())
                .companyId(companyDataCenter.getCompany().getId())
                .companyName(companyDataCenter.getCompany().getName())
                .dataCenterId(companyDataCenter.getDataCenter().getId())
                .dataCenterName(companyDataCenter.getDataCenter().getName())
                .description(companyDataCenter.getDescription())
                .grantedBy(companyDataCenter.getGrantedBy())
                .createdAt(companyDataCenter.getCreatedAt())
                .build();
    }
}