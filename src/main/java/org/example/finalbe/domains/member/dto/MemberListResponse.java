package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.member.domain.Member;

import java.time.LocalDateTime;

/**
 * 회원 목록 조회 응답 DTO
 */
@Builder
public record MemberListResponse(
        Long id,
        String userName,           // 로그인 아이디
        String name,               // 사용자 이름
        String email,              // 사용자 이메일
        Role role,                 // 권한 (ADMIN, OPERATOR, VIEWER)
        LocalDateTime lastLoginAt  // 마지막 로그인 (refreshTokenExpiryDate 기준)
) {
    /**
     * Entity를 DTO로 변환
     */
    public static MemberListResponse from(Member member) {
        return MemberListResponse.builder()
                .id(member.getId())
                .userName(member.getUserName())
                .name(member.getName())
                .email(member.getEmail())
                .role(member.getRole())
                .lastLoginAt(member.getRefreshTokenExpiryDate())
                .build();
    }
}