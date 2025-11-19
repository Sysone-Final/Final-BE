package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 회원 권한 변경 요청 DTO
 * OPERATOR가 VIEWER를 OPERATOR로 변경할 때 사용
 */
public record MemberRoleChangeRequest(
        @NotBlank(message = "권한을 입력해주세요.")
        @Pattern(regexp = "OPERATOR", message = "VIEWER는 OPERATOR 권한으로만 변경 가능합니다.")
        String role
) {
}