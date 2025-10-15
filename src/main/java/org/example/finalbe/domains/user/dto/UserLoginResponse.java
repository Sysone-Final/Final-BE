package org.example.finalbe.domains.user.dto;

import lombok.Builder;
import org.example.finalbe.domains.user.domain.User;

@Builder
public record UserLoginResponse(
        String accessToken,
        String refreshToken,
        String userId,
        String username,
        String role
) {
    public static UserLoginResponse from(User user, String accessToken, String refreshToken) {
        return UserLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}