package org.example.finalbe.domains.companydatacenter.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CompanyDataCenterCreateRequest(
        Long companyId,
        List<Long> dataCenterIds,
        String description
) {
}
