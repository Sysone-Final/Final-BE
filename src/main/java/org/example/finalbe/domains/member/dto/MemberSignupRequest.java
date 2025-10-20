package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.member.domain.Member;

/**
 * 회원가입 요청 DTO
 *
 * 개선사항:
 * - Bean Validation 추가
 * - 비밀번호, 이메일 형식 검증
 * - 필수 필드 명확화
 */
@Builder
public record MemberSignupRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 4, max = 50, message = "아이디는 4자 이상 50자 이하여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "아이디는 영문, 숫자, 하이픈, 언더스코어만 사용 가능합니다.")
        String userName,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
        String password,

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.")
        String name,

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
        String email,

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
        String phone,

        @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다.")
        String department,

        @Size(max = 100, message = "직급은 100자를 초과할 수 없습니다.")
        String position,

        @NotNull(message = "회사를 선택해주세요.")
        @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
        Long companyId,

        String role  // ADMIN이 설정, 기본값은 VIEWER
) {
    /**
     * 엔티티 변환 메서드
     * Request DTO의 일관된 패턴
     *
     * @param encodedPassword 암호화된 비밀번호
     * @param company 회사 엔티티
     * @return Member 엔티티
     */
    public Member toEntity(String encodedPassword, Company company) {
        return Member.builder()
                .userName(this.userName)
                .password(encodedPassword)
                .name(this.name)
                .email(this.email)
                .phone(this.phone)
                .department(this.department)
                .company(company)
                .role(this.role != null ? Role.valueOf(this.role) : Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}