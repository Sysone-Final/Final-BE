// 작성자: 황요한
// 클래스: 회원 권한 변경 요청 DTO
// role: 변경할 권한(OPERATOR만 가능)

package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 회원 권한 변경 요청 DTO
 */
public record MemberRoleChangeRequest(
        @NotBlank(message = "권한을 입력해주세요.")
        @Pattern(regexp = "OPERATOR", message = "VIEWER는 OPERATOR 권한으로만 변경 가능합니다.")
        String role
) {
}
