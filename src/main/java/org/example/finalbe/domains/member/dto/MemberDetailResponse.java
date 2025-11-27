/**
 * 작성자: 황요한
 * 회원 상세 정보 응답 DTO
 */
package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.member.domain.Member;

import java.time.LocalDateTime;

@Builder
public record MemberDetailResponse(
        Long id,
        String userName,
        String name,
        String email,
        String phone,
        String city,
        String street,
        String zipcode,
        Role role,
        UserStatus status,
        Long companyId,
        String companyName,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
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