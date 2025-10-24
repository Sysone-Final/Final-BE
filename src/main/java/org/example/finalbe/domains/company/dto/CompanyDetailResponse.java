package org.example.finalbe.domains.company.dto;

import lombok.Builder;

import org.example.finalbe.domains.company.domain.Company;
import java.time.LocalDateTime;

@Builder
public record CompanyDetailResponse(
        Long id,
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
        String logoUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CompanyDetailResponse from(Company company) {
        return CompanyDetailResponse.builder()
                .id(company.getId())
                .code(company.getCode())
                .name(company.getName())
                .businessNumber(company.getBusinessNumber())
                .ceoName(company.getCeoName())
                .phone(company.getPhone())
                .fax(company.getFax())
                .email(company.getEmail())
                .address(company.getAddress())
                .website(company.getWebsite())
                .industry(company.getIndustry())
                .description(company.getDescription())
                .employeeCount(company.getEmployeeCount())
                .establishedDate(company.getEstablishedDate())
                .logoUrl(company.getLogoUrl())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}