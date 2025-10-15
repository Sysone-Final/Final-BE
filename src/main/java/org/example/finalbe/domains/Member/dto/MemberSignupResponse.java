package org.example.finalbe.domains.Member.dto;

import lombok.Builder;
import org.example.finalbe.domains.Member.domain.Member;

@Builder
public record MemberSignupResponse(
        Long userId,
        String username,
        String message
) {
    public static MemberSignupResponse from(Member member, String message) {
        return MemberSignupResponse.builder()
                .userId(member.getId())
                .username(member.getUsername())
                .message(message)
                .build();
    }
}