package org.example.finalbe.domains.user.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.user.domain.Address;
import org.example.finalbe.domains.user.domain.User;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record UserSignupRequest(
        String username,
        String password,
        String name,
        String email,
        String phone,
        String profileImgUrl,
        Address address,
        MultipartFile profileImage,
        Boolean verified,
        Role role
) {
    public User toEntity(String encodedPassword, String uploadedImageUrl) {
        return User.builder()
                .username(this.username)
                .password(encodedPassword)
                .name(this.name)
                .email(this.email)
                .phone(this.phone)
                .address(this.address)
                .status(Boolean.TRUE.equals(this.verified) ? UserStatus.ACTIVE : UserStatus.INACTIVE)
                .role(this.role != null ? this.role : Role.VIEWER)
                .build();
    }
}