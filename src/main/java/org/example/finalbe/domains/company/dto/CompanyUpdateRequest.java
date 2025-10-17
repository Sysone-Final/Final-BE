package org.example.finalbe.domains.company.dto;

import lombok.Builder;

@Builder
public record CompanyUpdateRequest(
        String name,
        String businessNumber,
        String ceoName,
        String phone,
        String fax,
        String email,
        String address,
        String website,
        String industry,
        String description,
        Integer employeeCount,
        String establishedDate,
        String logoUrl
) {
}