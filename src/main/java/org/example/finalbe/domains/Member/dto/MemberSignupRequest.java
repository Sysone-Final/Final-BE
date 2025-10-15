package org.example.finalbe.domains.Member.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.Member.domain.Address;
import org.example.finalbe.domains.Member.domain.Member;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record MemberSignupRequest(
        String username,
        String password,
        String name,
        String email,
        String phone,
        String profileImgUrl,
        Address address,
        MultipartFile profileImage,
        Role role
) {
    public Member toEntity(String encodedPassword) {
        return Member.builder()
                .username(this.username)
                .password(encodedPassword)
                .name(this.name)
                .email(this.email)
                .phone(this.phone)
                .address(this.address)
                .status(UserStatus.ACTIVE)
                .role(this.role != null ? this.role : Role.VIEWER)
                .build();
    }
}