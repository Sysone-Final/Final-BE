package org.example.finalbe.domains.member.dto;

import org.example.finalbe.domains.member.domain.Member;

/**
 * 로그인 응답 DTO
 * Access Token은 응답 바디에, Refresh Token은 HTTP-Only Cookie로 전달
 */
public record MemberLoginResponse(
        String accessToken,
        Long id,
        String userName,
        String name,
        String role,
        Long companyId
) {
    public static MemberLoginResponse from(Member member, String accessToken) {
        return new MemberLoginResponse(
                accessToken,
                member.getId(),
                member.getUserName(),
                member.getName(),
                member.getRole().name(),
                member.getCompany().getId()
        );
    }
}