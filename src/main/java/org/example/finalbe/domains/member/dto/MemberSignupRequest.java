package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.*;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.member.domain.Member;

/**
 * 회원가입 요청 DTO
 */
public record MemberSignupRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
        String userName,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 50, message = "이름은 최대 50자까지 입력 가능합니다.")
        String name,

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        String phone,


        @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
        Long companyId,

        String role
) {
    /**
     * DTO를 Entity로 변환
     */
    public Member toEntity(String encodedPassword, Company company) {
        return Member.builder()
                .userName(this.userName)
                .password(encodedPassword)
                .name(this.name)
                .email(this.email)
                .phone(this.phone)
                .company(company)
                .role(this.role != null ? Role.valueOf(this.role) : Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}