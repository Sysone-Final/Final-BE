package org.example.finalbe.domains.department.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.department.domain.Department;

/**
 * 부서 생성 요청 DTO
 */
@Builder
public record DepartmentCreateRequest(
        @NotBlank(message = "부서 코드를 입력해주세요.")
        @Size(max = 50, message = "부서 코드는 50자를 초과할 수 없습니다.")
        String departmentCode,

        @NotBlank(message = "부서명을 입력해주세요.")
        @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다.")
        String departmentName,

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
        String description,

        Long parentDepartmentId,

        Long managerId,

        @Size(max = 200, message = "위치는 200자를 초과할 수 없습니다.")
        String location,

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 02-1234-5678)")
        String phone,

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
        String email,

        @NotNull(message = "회사를 선택해주세요.")
        @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
        Long companyId
) {
    /**
     * 엔티티 변환 메서드
     */
    public Department toEntity(Company company, String createdBy) {
        return Department.builder()
                .departmentCode(this.departmentCode)
                .departmentName(this.departmentName)
                .description(this.description)
                .parentDepartmentId(this.parentDepartmentId)
                .managerId(this.managerId)
                .location(this.location)
                .phone(this.phone)
                .email(this.email)
                .company(company)
                .createdBy(createdBy)
                .build();
    }
}