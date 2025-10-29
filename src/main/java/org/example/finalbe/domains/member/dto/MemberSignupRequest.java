package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.member.domain.Member;

/**
 * 회원가입 요청 DTO
 * 클라이언트로부터 회원가입 정보를 받는 데이터 전송 객체
 *
 * 사용 기술:
 * - Bean Validation: Jakarta Validation API로 입력값 검증
 * - Record: Java 14 이상의 불변 객체 (DTO에 적합)
 * - Lombok Builder: 빌더 패턴으로 객체 생성
 * - Regular Expression: 정규식으로 형식 검증
 */
@Builder // Lombok의 빌더 패턴 적용 (가독성 좋은 객체 생성)
// 빌더 패턴을 사용하면 MemberSignupRequest.builder().userName("test").build() 형식으로 생성 가능
public record MemberSignupRequest( // record: 불변 객체 (Java 14 이상)
                                   // record는 final 필드만 가지며, 자동으로 생성자, getter, equals, hashCode, toString 생성
                                   // DTO는 데이터 전달만 담당하므로 불변 객체가 적합

                                   // === 아이디 필드 ===
                                   @NotBlank(message = "아이디를 입력해주세요.") // null, 빈 문자열(""), 공백만 있는 문자열("   ") 모두 불허
                                   // @NotBlank는 문자열 전용 검증 어노테이션
                                   // @NotNull은 null만 체크, @NotEmpty는 null과 빈 문자열만 체크

                                   @Size(min = 4, max = 50, message = "아이디는 4자 이상 50자 이하여야 합니다.") // 길이 제약
                                   // 최소 4자, 최대 50자 제한 (보안상 너무 짧거나 긴 아이디 방지)

                                   @Pattern(regexp = "^[a-zA-Z0-9_-]+$", // 정규식으로 허용 문자 제한
                                           message = "아이디는 영문, 숫자, 하이픈, 언더스코어만 사용 가능합니다.")
                                   // ^ : 문자열 시작, $ : 문자열 끝
                                   // [a-zA-Z0-9_-]+ : 영문 대소문자, 숫자, 언더스코어, 하이픈만 허용 (1개 이상)
                                   // + : 1회 이상 반복
                                   String userName, // 로그인용 아이디

                                   // === 비밀번호 필드 ===
                                   @NotBlank(message = "비밀번호를 입력해주세요.") // 비밀번호는 필수

                                   @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.") // 길이 제약
                                   // 최소 8자 이상 (보안 강화)
                                   // 최대 100자 (BCrypt 암호화 후 저장 공간 고려)

                                   @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$", // 복잡한 비밀번호 강제
                                           message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
                                   // (?=.*[A-Za-z]) : 영문 대소문자 1개 이상 포함 (Positive Lookahead)
                                   // (?=.*\\d) : 숫자 1개 이상 포함
                                   // (?=.*[@$!%*#?&]) : 특수문자 1개 이상 포함
                                   // [A-Za-z\\d@$!%*#?&]{8,} : 영문, 숫자, 특수문자로 8자 이상
                                   String password, // 평문 비밀번호 (Service에서 BCrypt로 암호화됨)

                                   // === 이름 필드 ===
                                   @NotBlank(message = "이름을 입력해주세요.") // 이름은 필수

                                   @Size(max = 100, message = "이름은 100자를 초과할 수 없습니다.") // 최대 길이 제한
                                   // 한글 이름은 보통 2~10자 정도이지만, 외국인 이름을 고려하여 100자로 설정
                                   String name, // 회원의 실제 이름

                                   // === 이메일 필드 (선택사항) ===
                                   @Email(message = "올바른 이메일 형식이 아닙니다.") // 이메일 형식 검증
                                   // @Email은 RFC 5322 표준에 따라 이메일 형식을 검증
                                   // 예: user@example.com, user.name@example.co.kr

                                   @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.") // 최대 길이 제한
                                   String email, // 이메일 주소 (nullable)

                                   // === 전화번호 필드 (선택사항) ===
                                   @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", // 전화번호 형식 강제
                                           message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
                                   // \\d{2,3} : 지역번호 2~3자리 (02, 010, 031 등)
                                   // - : 하이픈
                                   // \\d{3,4} : 국번 3~4자리
                                   // \\d{4} : 번호 4자리
                                   String phone, // 전화번호 (nullable, 하이픈 포함 형식)

                                   // === 직급 필드 (선택사항) ===
                                   @Size(max = 100, message = "직급은 100자를 초과할 수 없습니다.") // 최대 길이 제한
                                   String position, // 직급 (예: 사원, 대리, 과장, 부장 등)

                                   // === 회사 ID 필드 ===
                                   @NotNull(message = "회사를 선택해주세요.") // null 불허 (회사는 필수)
                                   // @NotNull은 모든 타입에 사용 가능 (숫자, 객체 등)

                                   @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // 최소값 제약
                                   // ID는 1 이상이어야 함 (0 이하는 유효하지 않은 ID)
                                   Long companyId, // 회사 ID (외래키)

                                   // === 권한 필드 (선택사항) ===
                                   String role // 권한 (ADMIN, OPERATOR, VIEWER)
                                   // ADMIN이 설정할 때만 사용, 일반 회원가입 시에는 null (기본값: VIEWER)
) {
    /**
     * DTO를 Entity로 변환하는 메서드
     *
     * @param encodedPassword BCrypt로 암호화된 비밀번호 (평문 X)
     * @param company 조회된 Company 엔티티 객체
     * @return Member 엔티티 객체
     *
     * 이 메서드가 필요한 이유:
     * - DTO는 요청 데이터를 받는 역할, Entity는 실제 DB에 저장되는 역할
     * - 계층 간 책임 분리: Controller는 DTO만, Service는 Entity만 다룸
     * - Entity 생성 로직을 DTO에 캡슐화하여 재사용성 향상
     */
    public Member toEntity(String encodedPassword, Company company) {
        // === Member 엔티티 빌더 패턴으로 생성 ===
        return Member.builder() // Member 빌더 시작

                // === 필수 필드 설정 ===
                .userName(this.userName) // DTO의 userName을 Entity에 설정
                // this.userName: record의 필드 접근 (자동 생성된 getter)

                .password(encodedPassword) // 암호화된 비밀번호 (평문이 아님!)
                // Service에서 passwordEncoder.encode()로 암호화된 값 전달받음

                .name(this.name) // 이름 설정

                .email(this.email) // 이메일 설정 (null 가능)

                .phone(this.phone) // 전화번호 설정 (null 가능)

                .company(company) // 회사 엔티티 설정 (연관 관계)
                // Company 엔티티 객체를 받아서 Member와 연관 관계 설정
                // JPA가 company_id 외래키로 자동 매핑

                // === 권한 설정 ===
                .role(this.role != null ? Role.valueOf(this.role) : Role.VIEWER) // 권한 설정
                // 삼항 연산자 사용: role이 null이 아니면 문자열을 Enum으로 변환, null이면 기본값 VIEWER
                // Role.valueOf(): 문자열을 Enum 타입으로 변환
                // 예: "ADMIN" -> Role.ADMIN

                // === 계정 상태 설정 ===
                .status(UserStatus.ACTIVE) // 회원가입 시 항상 활성 상태로 시작
                // UserStatus.ACTIVE: 활성 계정
                // 관리자가 나중에 INACTIVE로 변경 가능

                .build(); // Member 엔티티 객체 최종 생성
        // 빌더 패턴의 build()는 설정된 값으로 최종 객체를 생성
    }
}