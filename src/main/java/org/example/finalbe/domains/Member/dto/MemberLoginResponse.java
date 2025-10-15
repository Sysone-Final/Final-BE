package org.example.finalbe.domains.Member.dto;

import lombok.Builder;
import org.example.finalbe.domains.Member.domain.Member;

@Builder
public record MemberLoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        String role
) {
    public static MemberLoginResponse from(Member member, String accessToken, String refreshToken) {
        return MemberLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(member.getId())
                .username(member.getUsername())
                .role(member.getRole().name())
                .build();
    }
}