// 작성자: 황요한
// 클래스: 회원가입 응답 DTO (회원 생성 결과 전달)

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
    // Member 엔티티를 응답 DTO로 변환
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
