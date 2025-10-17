package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.member.domain.Member;

@Builder
public record MemberLoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        String role,
        Long companyId,      // 추가
        String companyName   // 추가
) {
    public static MemberLoginResponse from(Member member, String accessToken, String refreshToken) {
        return MemberLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(member.getId())
                .username(member.getUsername())
                .role(member.getRole().name())
                .companyId(member.getCompany().getId())        // 추가
                .companyName(member.getCompany().getName())    // 추가
                .build();
    }
}