package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.time.LocalDateTime;

@Builder
public record CompanyDataCenterListResponse(
        Long dataCenterId,
        String dataCenterName,
        String dataCenterCode,
        String location,
        String floor,
        LocalDateTime grantedAt
) {
    public static CompanyDataCenterListResponse from(DataCenter dataCenter, LocalDateTime grantedAt) {
        return CompanyDataCenterListResponse.builder()
                .dataCenterId(dataCenter.getId())
                .dataCenterName(dataCenter.getName())
                .dataCenterCode(dataCenter.getCode())
                .location(dataCenter.getLocation())
                .floor(dataCenter.getFloor())
                .grantedAt(grantedAt)
                .build();
    }
}
