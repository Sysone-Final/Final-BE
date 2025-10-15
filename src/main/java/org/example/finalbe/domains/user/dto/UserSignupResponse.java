package org.example.finalbe.domains.user.dto;

import lombok.Builder;
import org.example.finalbe.domains.user.domain.User;

@Builder
public record UserSignupResponse(
        String userId,
        String username,
        String message
) {
    public static UserSignupResponse from(User user, String message) {
        return UserSignupResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .message(message)
                .build();
    }
}