// 작성자: 황요한
// 회원 목록 조회 응답을 전달하는 DTO

package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.member.domain.Member;

import java.time.LocalDateTime;

@Builder
public record MemberListResponse(
        Long id,
        String userName,
        String name,
        String email,
        Role role,
        LocalDateTime lastLoginAt
) {

    // Entity를 DTO로 변환하는 메서드
    public static MemberListResponse from(Member member) {
        return MemberListResponse.builder()
                .id(member.getId())
                .userName(member.getUserName())
                .name(member.getName())
                .email(member.getEmail())
                .role(member.getRole())
                .lastLoginAt(member.getLastLoginAt())
                .build();
    }
}
