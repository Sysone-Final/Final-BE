// 작성자: 황요한
// 로그인 성공 시 사용자 및 토큰 정보를 반환하는 DTO

package org.example.finalbe.domains.member.dto;

import org.example.finalbe.domains.member.domain.Member;

public record MemberLoginResponse(
        String accessToken,
        Long id,
        String userName,
        String name,
        String role,
        Long companyId,
        String companyName
) {
    public static MemberLoginResponse from(Member member, String accessToken) {
        return new MemberLoginResponse(
                accessToken,
                member.getId(),
                member.getUserName(),
                member.getName(),
                member.getRole().name(),
                member.getCompany().getId(),
                member.getCompany().getName()
        );
    }
}
