package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;

@Builder
public record CompanyCreateRequest(
        String code,
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
    public Company toEntity() {
        return Company.builder()
                .code(this.code)
                .name(this.name)
                .businessNumber(this.businessNumber)
                .ceoName(this.ceoName)
                .phone(this.phone)
                .fax(this.fax)
                .email(this.email)
                .address(this.address)
                .website(this.website)
                .industry(this.industry)
                .description(this.description)
                .employeeCount(this.employeeCount)
                .establishedDate(this.establishedDate)
                .logoUrl(this.logoUrl)
                .build();
    }
}
