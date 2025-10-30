package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 */
public record MemberLoginRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        String userName,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}