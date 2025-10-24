package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급 요청 DTO
 */
public record TokenRefreshRequest(
        String refreshToken
) {
}