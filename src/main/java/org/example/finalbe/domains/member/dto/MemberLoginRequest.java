package org.example.finalbe.domains.member.dto;

import lombok.Builder;

@Builder
public record MemberLoginRequest(
        String userName,
        String password
) {
}