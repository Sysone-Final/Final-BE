package org.example.finalbe.domains.department.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 회원-부서 매핑 생성 요청 DTO
 */
@Builder
public record MemberDepartmentCreateRequest(
        @NotNull(message = "회원 ID를 입력해주세요.")
        @Min(value = 1, message = "유효하지 않은 회원 ID입니다.")
        Long memberId,

        @NotNull(message = "부서 ID를 입력해주세요.")
        @Min(value = 1, message = "유효하지 않은 부서 ID입니다.")
        Long departmentId,

        Boolean isPrimary,

        @Size(max = 100, message = "직급은 100자를 초과할 수 없습니다.")
        String position,

        LocalDate joinDate
) {
}