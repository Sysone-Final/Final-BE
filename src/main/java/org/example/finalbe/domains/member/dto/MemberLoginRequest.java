// 작성자: 황요한
// 로그인 요청 정보를 전달하는 DTO

package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberLoginRequest(

        @NotBlank(message = "아이디를 입력해주세요.")
        String userName,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
