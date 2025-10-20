package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 로그인 요청 DTO
 *
 * 개선사항:
 * - Bean Validation 추가
 * - 필수 입력값 검증
 */
@Builder
public record MemberLoginRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(max = 50, message = "아이디는 50자를 초과할 수 없습니다.")
        String userName,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(max = 100, message = "비밀번호는 100자를 초과할 수 없습니다.")
        String password
) {
}