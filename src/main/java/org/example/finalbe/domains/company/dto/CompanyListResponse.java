/**
 * 작성자: 황요한
 * 회사 목록 조회 응답 DTO
 */
package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;

@Builder
public record CompanyListResponse(
        Long id,
        String code,
        String name,
        String businessNumber,
        String phone,
        String industry,
        Integer employeeCount
) {
    /**
     * Entity → DTO 변환
     */
    public static CompanyListResponse from(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("Company 엔티티가 null입니다.");
        }

        return CompanyListResponse.builder()
                .id(company.getId())
                .code(company.getCode())
                .name(company.getName())
                .businessNumber(company.getBusinessNumber())
                .phone(company.getPhone())
                .industry(company.getIndustry())
                .employeeCount(company.getEmployeeCount())
                .build();
    }
}