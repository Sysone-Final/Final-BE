package org.example.finalbe.domains.member.dto;

/**
 * 토큰 재발급 응답 DTO
 */
public record MemberRefreshResponse(
        String accessToken,
        String message
) {
}