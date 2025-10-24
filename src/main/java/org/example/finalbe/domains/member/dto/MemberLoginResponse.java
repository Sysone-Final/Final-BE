package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.member.domain.Member;

/**
 * 로그인 응답 DTO
 * ⚠️ refreshToken은 httpOnly Cookie로 전달하므로 제외
 */
@Builder
public record MemberLoginResponse(
        Long id,
        String userName,
        String name,
        String email,
        String role,
        String companyName,
        String accessToken,
        String message
) {
    public static MemberLoginResponse from(Member member, String accessToken) {
        return MemberLoginResponse.builder()
                .id(member.getId())
                .userName(member.getUserName())
                .name(member.getName())
                .email(member.getEmail())
                .role(member.getRole().name())
                .companyName(member.getCompany().getName())
                .accessToken(accessToken)
                .message("로그인 성공")
                .build();
    }
}