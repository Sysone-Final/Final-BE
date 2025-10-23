package org.example.finalbe.domains.department.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 부서 수정 요청 DTO
 */
@Builder
public record DepartmentUpdateRequest(
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
        String email
) {
}