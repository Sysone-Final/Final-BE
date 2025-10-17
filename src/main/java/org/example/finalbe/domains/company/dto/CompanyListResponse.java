package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;


@Builder
public record CompanyListResponse(
        Long id,
        String code,
        String name,
        String businessNumber,
        String ceoName,
        String phone,
        String email,
        String industry
) {
    public static CompanyListResponse from(Company company) {
        return CompanyListResponse.builder()
                .id(company.getId())
                .code(company.getCode())
                .name(company.getName())
                .businessNumber(company.getBusinessNumber())
                .ceoName(company.getCeoName())
                .phone(company.getPhone())
                .email(company.getEmail())
                .industry(company.getIndustry())
                .build();
    }
}