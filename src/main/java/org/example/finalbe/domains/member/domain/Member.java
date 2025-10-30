package org.example.finalbe.domains.member.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.company.domain.Company;

import java.time.LocalDateTime;

/**
 * 회원 엔티티
 */
@Entity
@Table(name = "member")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id; // 회원 ID

    @Column(name = "user_name", nullable = false, unique = true, length = 100)
    private String userName; // 로그인 아이디

    @Column(name = "password", nullable = false, length = 250)
    private String password; // 암호화된 비밀번호

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 이름

    @Column(name = "email", length = 100)
    private String email; // 이메일

    @Column(name = "phone", length = 20)
    private String phone; // 전화번호

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE; // 계정 상태 (ACTIVE, INACTIVE, DELETED)

    @Embedded
    private Address address; // 주소 (city, street, zipcode)

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.VIEWER; // 권한 (ADMIN, OPERATOR, VIEWER)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // 소속 회사

    @Column(name = "department", length = 100)
    private String department; // 부서명

    @Column(name = "refresh_token", length = 500)
    private String refreshToken; // 리프레시 토큰

    @Column(name = "refresh_token_expiry_date")
    private LocalDateTime refreshTokenExpiryDate; // 리프레시 토큰 만료일시

    /**
     * Refresh Token 업데이트
     */
    public void updateRefreshToken(String refreshToken, LocalDateTime expiryDate) {
        this.refreshToken = refreshToken;
        this.refreshTokenExpiryDate = expiryDate;
    }

    /**
     * Refresh Token 삭제
     */
    public void clearRefreshToken() {
        this.refreshToken = null;
        this.refreshTokenExpiryDate = null;
    }

    /**
     * Refresh Token 유효성 검증
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        if (this.refreshToken == null || this.refreshTokenExpiryDate == null) {
            return false;
        }
        return this.refreshToken.equals(refreshToken)
                && this.refreshTokenExpiryDate.isAfter(LocalDateTime.now());
    }

    /**
     * 회원 정보 수정
     */
    public void updateInfo(String name, String email, String phone, String department) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        if (email != null) {
            this.email = email;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (department != null) {
            this.department = department;
        }
    }

    /**
     * 비밀번호 변경
     */
    public void changePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
    }

    /**
     * 계정 상태 변경
     */
    public void updateStatus(UserStatus status) {
        this.status = status;
    }
}