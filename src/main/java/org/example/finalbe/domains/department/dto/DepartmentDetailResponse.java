package org.example.finalbe.domains.department.dto;

import lombok.Builder;
import org.example.finalbe.domains.department.domain.Department;

import java.time.LocalDateTime;

/**
 * 부서 상세 조회 응답 DTO
 */
@Builder
public record DepartmentDetailResponse(
        Long id,
        String departmentCode,
        String departmentName,
        String description,
        Long parentDepartmentId,
        Long managerId,
        String location,
        String phone,
        String email,
        Integer employeeCount,
        Long companyId,
        String companyName,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DepartmentDetailResponse from(Department department) {
        return DepartmentDetailResponse.builder()
                .id(department.getId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .description(department.getDescription())
                .parentDepartmentId(department.getParentDepartmentId())
                .managerId(department.getManagerId())
                .location(department.getLocation())
                .phone(department.getPhone())
                .email(department.getEmail())
                .employeeCount(department.getEmployeeCount())
                .companyId(department.getCompany().getId())
                .companyName(department.getCompany().getName())
                .createdBy(department.getCreatedBy())
                .updatedBy(department.getUpdatedBy())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}