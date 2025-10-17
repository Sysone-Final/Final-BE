package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record MemberLogoutResponse(
        String message,
        LocalDateTime logoutTime
) {
    public static MemberLogoutResponse of(String message) {
        return MemberLogoutResponse.builder()
                .message(message)
                .logoutTime(LocalDateTime.now())
                .build();
    }
}