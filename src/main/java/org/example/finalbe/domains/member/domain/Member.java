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
 *
 * - JPA(Hibernate): 객체-관계 매핑을 통해 Member 테이블과 매핑
 * - Lombok: @Getter, @Builder 등으로 보일러플레이트 코드 제거
 * - BCrypt: 비밀번호 암호화 (Service 계층에서 처리)
 */
@Entity // JPA 엔티티임을 선언. 이 클래스는 데이터베이스 member 테이블과 매핑됨
@Table(name = "member") // 실제 데이터베이스 테이블 이름 지정
@NoArgsConstructor // JPA는 기본 생성자가 필요함 (Hibernate가 리플렉션으로 객체 생성 시 사용)
@AllArgsConstructor // 모든 필드를 받는 생성자 (Builder 패턴과 함께 사용)
@Getter // 모든 필드의 getter 메서드 자동 생성
@Builder // 빌더 패턴을 사용하여 객체 생성 (가독성 향상)
public class Member extends BaseTimeEntity { // BaseTimeEntity를 상속받아 createdAt, updatedAt 자동 관리

    @Id // Primary Key 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment 전략 (PostgreSQL의 SERIAL 타입 사용)
    @Column(name = "member_id") // 데이터베이스 컬럼 이름 명시
    private Long id; // 회원 고유 식별자

    @Column(name = "user_name", nullable = false, unique = true, length = 100) // NOT NULL, UNIQUE 제약조건
    private String userName; // 로그인용 아이디 (중복 불가)

    @Column(name = "password", nullable = false, length = 250) // BCrypt 암호화된 비밀번호는 60자 이상
    private String password; // 암호화된 비밀번호 (BCrypt 해시)

    @Column(name = "name", nullable = false, length = 100) // 회원 이름은 필수
    private String name; // 회원의 실제 이름

    @Column(name = "email", length = 100) // 이메일은 선택사항
    private String email; // 이메일 주소

    @Column(name = "phone", length = 20) // 전화번호는 선택사항
    private String phone; // 전화번호

    @Enumerated(EnumType.STRING) // Enum을 문자열로 저장 (ORDINAL은 순서가 바뀌면 문제 발생)
    @Column(name = "status", length = 20, nullable = false) // 계정 상태는 필수
    @Builder.Default // Builder 사용 시 기본값 설정
    private UserStatus status = UserStatus.ACTIVE; // 계정 상태 (ACTIVE, INACTIVE, DELETED)

    @Embedded // Address는 별도 테이블이 아닌 Member 테이블에 포함됨
    private Address address; // 주소 정보 (임베디드 타입)

    @Enumerated(EnumType.STRING) // Role을 문자열로 저장
    @Column(name = "role", nullable = false, length = 20) // 권한은 필수
    @Builder.Default // Builder 사용 시 기본값 설정
    private Role role = Role.VIEWER; // 권한 (ADMIN, OPERATOR, VIEWER)

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계, 지연 로딩 (필요할 때만 조회)
    @JoinColumn(name = "company_id", nullable = false) // 외래키 컬럼 지정
    private Company company; // 소속 회사 (필수)

    @Column(name = "department", length = 100) // 부서는 선택사항 (향후 MemberDepartment로 분리 가능)
    private String department; // 부서명

    // === Refresh Token 관련 필드 (Redis 제거 후 추가) ===

    @Column(name = "refresh_token", length = 500) // Refresh Token은 JWT이므로 긴 문자열
    private String refreshToken; // Refresh Token 저장 (로그인 시 생성, 로그아웃 시 삭제)

    @Column(name = "refresh_token_expiry_date") // 만료 시간
    private LocalDateTime refreshTokenExpiryDate; // Refresh Token 만료 시간

    // === 비즈니스 로직 메서드 ===

    /**
     * Refresh Token 업데이트
     * 로그인 또는 토큰 재발급 시 호출
     */
    public void updateRefreshToken(String refreshToken, LocalDateTime expiryDate) {
        this.refreshToken = refreshToken; // 새로운 Refresh Token으로 갱신
        this.refreshTokenExpiryDate = expiryDate; // 만료 시간 갱신
    }

    /**
     * Refresh Token 삭제
     * 로그아웃 시 호출하여 Refresh Token을 무효화
     */
    public void clearRefreshToken() {
        this.refreshToken = null; // Refresh Token 제거
        this.refreshTokenExpiryDate = null; // 만료 시간 제거
    }

    /**
     * Refresh Token 유효성 검증
     * 저장된 토큰과 만료 시간을 확인
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        // 저장된 Refresh Token이 없으면 false
        if (this.refreshToken == null || this.refreshTokenExpiryDate == null) {
            return false;
        }
        // 입력받은 토큰이 저장된 토큰과 일치하고, 만료 시간이 현재 시간보다 이후면 true
        return this.refreshToken.equals(refreshToken)
                && this.refreshTokenExpiryDate.isAfter(LocalDateTime.now());
    }

    /**
     * 회원 정보 수정
     * 이름, 이메일, 전화번호, 부서 등을 수정할 때 사용
     */
    public void updateInfo(String name, String email, String phone, String department) {
        // null이 아닌 값만 업데이트 (부분 수정 가능)
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
     * 새로운 암호화된 비밀번호로 갱신
     */
    public void changePassword(String newEncodedPassword) {
        this.password = newEncodedPassword; // 이미 암호화된 비밀번호를 받음 (Service에서 암호화 처리)
    }

    /**
     * 계정 상태 변경
     * 관리자가 계정을 활성화/비활성화할 때 사용
     */
    public void updateStatus(UserStatus status) {
        this.status = status;
    }
}