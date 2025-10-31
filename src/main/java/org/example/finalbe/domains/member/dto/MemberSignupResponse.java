package org.example.finalbe.domains.member.dto;

import org.example.finalbe.domains.member.domain.Member;

/**
 * 회원가입 응답 DTO
 */
public record MemberSignupResponse(
        Long id,
        String userName,
        String name,
        String email,
        String role,
        String message
) {
    public static MemberSignupResponse from(Member member, String message) {
        return new MemberSignupResponse(
                member.getId(),
                member.getUserName(),
                member.getName(),
                member.getEmail(),
                member.getRole().name(),
                message
        );
    }
}