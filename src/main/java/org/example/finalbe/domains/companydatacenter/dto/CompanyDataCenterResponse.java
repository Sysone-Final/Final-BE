package org.example.finalbe.domains.companydatacenter.dto;

import lombok.Builder;
import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;

import java.time.LocalDateTime;

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