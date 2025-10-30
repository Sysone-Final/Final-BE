package org.example.finalbe.domains.member.dto;

/**
 * 로그아웃 응답 DTO
 */
public record MemberLogoutResponse(
        String userName,
        String message
) {
}