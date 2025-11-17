package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원 정보 수정 요청 DTO
 * null인 필드는 수정하지 않음 (부분 수정 지원)
 */
public record MemberUpdateRequest(
        @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
        String userName,

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        String phone,

        String city,      // 도시

        String street,    // 상세 주소

        String zipcode,   // 우편번호

        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        String password   // 새 비밀번호
) {
}