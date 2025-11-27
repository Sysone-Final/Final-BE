// 작성자: 황요한
// 클래스: MemberUpdateRequest - 회원 정보 수정 요청 DTO

package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record MemberUpdateRequest(
        @Size(min = 4, max = 20)
        @Pattern(regexp = "^[a-zA-Z0-9]+$")
        String userName,

        @Email
        @Size(max = 100)
        String email,

        @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$")
        String phone,

        @Size(min = 8, max = 20)
        String password,

        @Size(max = 100)
        String city,

        @Size(max = 200)
        String street,

        @Size(max = 10)
        String zipcode,

        @Pattern(regexp = "^(ADMIN|OPERATOR|VIEWER)$")
        String role
) {
}
