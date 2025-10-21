package org.example.finalbe.domains.member.dto;

import lombok.Builder;

/**
 * 토큰 재발급 응답 DTO
 * ⚠️ refreshToken은 httpOnly Cookie로 전달하므로 제외
 */
@Builder
public record TokenRefreshResponse(
        String accessToken,
        String message
) {
    public static TokenRefreshResponse of(String accessToken) {
        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .message("토큰 재발급 완료")
                .build();
    }
}