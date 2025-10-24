package org.example.finalbe.domains.department.dto;

import lombok.Builder;
import org.example.finalbe.domains.department.domain.Department;

import java.time.LocalDateTime;

/**
 * 부서 목록 조회 응답 DTO
 */
@Builder
public record DepartmentListResponse(
        Long id,
        String departmentCode,
        String departmentName,
        String description,
        String location,
        Integer employeeCount,
        String companyName,
        LocalDateTime createdAt
) {
    public static DepartmentListResponse from(Department department) {
        return DepartmentListResponse.builder()
                .id(department.getId())
                .departmentCode(department.getDepartmentCode())
                .departmentName(department.getDepartmentName())
                .description(department.getDescription())
                .location(department.getLocation())
                .employeeCount(department.getEmployeeCount())
                .companyName(department.getCompany().getName())
                .createdAt(department.getCreatedAt())
                .build();
    }
}