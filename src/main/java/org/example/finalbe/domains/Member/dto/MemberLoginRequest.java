package org.example.finalbe.domains.Member.dto;

import lombok.Builder;

@Builder
public record MemberLoginRequest(
        String username,
        String password
) {
}