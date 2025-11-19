package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.member.domain.Member;

import java.time.LocalDateTime;

/**
 * 회원 상세 정보 응답 DTO
 */
@Builder
public record MemberDetailResponse(
        Long id,
        String userName,           // 로그인 아이디
        String name,               // 사용자 이름
        String email,              // 사용자 이메일
        String phone,              // 전화번호
        String city,               // 도시
        String street,             // 상세 주소
        String zipcode,            // 우편번호
        Role role,                 // 권한 (ADMIN, OPERATOR, VIEWER)
        UserStatus status,         // 계정 상태
        Long companyId,            // 회사 ID
        String companyName,        // 회사명
        LocalDateTime lastLoginAt, // 마지막 로그인 시각
        LocalDateTime createdAt,   // 생성 시각
        LocalDateTime updatedAt    // 수정 시각
) {
    /**
     * Entity를 DTO로 변환
     */
    public static MemberDetailResponse from(Member member) {
        return MemberDetailResponse.builder()
                .id(member.getId())
                .userName(member.getUserName())
                .name(member.getName())
                .email(member.getEmail())
                .phone(member.getPhone())
                .city(member.getAddress() != null ? member.getAddress().getCity() : null)
                .street(member.getAddress() != null ? member.getAddress().getStreet() : null)
                .zipcode(member.getAddress() != null ? member.getAddress().getZipcode() : null)
                .role(member.getRole())
                .status(member.getStatus())
                .companyId(member.getCompany().getId())
                .companyName(member.getCompany().getName())
                .lastLoginAt(member.getLastLoginAt())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}