// src/main/java/org/example/finalbe/domains/member/dto/MemberUpdateRequest.java

package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 회원 정보 수정 요청 DTO (권한 변경 포함)
 */
@Builder
public record MemberUpdateRequest(
        @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문자와 숫자만 사용 가능합니다.")
        String userName,

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        String phone,

        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @Size(max = 100, message = "도시명은 100자 이하여야 합니다.")
        String city,

        @Size(max = 200, message = "상세 주소는 200자 이하여야 합니다.")
        String street,

        @Size(max = 10, message = "우편번호는 10자 이하여야 합니다.")
        String zipcode,

        // 권한 변경 필드 추가
        @Pattern(regexp = "^(ADMIN|OPERATOR|VIEWER)$", message = "권한은 ADMIN, OPERATOR, VIEWER 중 하나여야 합니다.")
        String role
) {
}