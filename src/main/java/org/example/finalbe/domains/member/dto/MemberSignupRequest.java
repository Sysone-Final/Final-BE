package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.member.domain.Address;
import org.example.finalbe.domains.member.domain.Member;
import org.springframework.web.multipart.MultipartFile;

@Builder
public record MemberSignupRequest(
        String userName,
        String password,
        String name,
        String email,
        String phone,
        String profileImgUrl,
        Address address,
        MultipartFile profileImage,
        Role role,
        Long companyId
) {
    public Member toEntity(String encodedPassword, Company company) {
        return Member.builder()
                .userName(this.userName)
                .password(encodedPassword)
                .name(this.name)
                .email(this.email)
                .phone(this.phone)
                .address(this.address)
                .status(UserStatus.ACTIVE)
                .role(this.role != null ? this.role : Role.VIEWER)
                .company(company)
                .build();
    }
}