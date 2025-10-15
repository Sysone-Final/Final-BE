package org.example.finalbe.domains.user.dto;

import lombok.Builder;

@Builder
public record UserLoginRequest(
        String username,
        String password
) {
}