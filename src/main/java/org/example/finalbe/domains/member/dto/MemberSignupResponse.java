package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.member.domain.Member;

@Builder
public record MemberSignupResponse(
        Long userId,
        String username,
        String companyName,  // 추가
        String message
) {
    public static MemberSignupResponse from(Member member, String message) {
        return MemberSignupResponse.builder()
                .userId(member.getId())
                .username(member.getUsername())
                .companyName(member.getCompany().getName())  // 추가
                .message(message)
                .build();
    }
}