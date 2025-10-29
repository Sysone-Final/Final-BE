package org.example.finalbe.domains.company.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DelYN;

/**
 * 회사 엔티티
 *
 * - JPA(Hibernate): 객체-관계 매핑을 통해 company 테이블과 매핑
 * - Lombok: @Getter, @Builder 등으로 보일러플레이트 코드 제거
 * - Soft Delete: delYn 필드로 논리 삭제 구현
 */
@Entity // JPA 엔티티 선언. 이 클래스는 데이터베이스 company 테이블과 매핑됨
@Table(name = "company", // 실제 데이터베이스 테이블 이름 지정
        indexes = { // 인덱스 정의로 조회 성능 향상
                @Index(name = "idx_company_name", columnList = "name"), // 회사명으로 검색 시 성능 향상
                @Index(name = "idx_company_code", columnList = "code")  // 회사 코드로 검색 시 성능 향상
        })
@NoArgsConstructor // JPA는 기본 생성자가 필요함 (Hibernate가 리플렉션으로 객체 생성 시 사용)
@AllArgsConstructor // 모든 필드를 받는 생성자 (Builder 패턴과 함께 사용)
@Getter // 모든 필드의 getter 메서드 자동 생성
@Builder // 빌더 패턴을 사용하여 객체 생성 (가독성 향상)
public class Company extends BaseTimeEntity { // BaseTimeEntity 상속으로 createdAt, updatedAt 자동 관리

    // === Primary Key ===
    @Id // Primary Key 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment 전략 (PostgreSQL의 SERIAL 타입 사용)
    @Column(name = "company_id") // 데이터베이스 컬럼 이름 명시
    private Long id; // 회사 고유 식별자

    // === 기본 정보 필드 ===
    @Column(name = "code", nullable = false, unique = true, length = 50) // NOT NULL, UNIQUE 제약조건
    // unique = true: 회사 코드는 중복 불가 (예: COMP001, COMP002)
    private String code; // 회사 코드 (시스템 내부 식별용)

    @Column(name = "name", nullable = false, length = 200) // 회사명은 필수
    private String name; // 회사명 (예: 테크놀로지 주식회사)

    @Column(name = "business_number", unique = true, length = 20) // 사업자등록번호도 중복 불가
    // unique = true: 사업자등록번호는 법적으로 고유함
    private String businessNumber; // 사업자등록번호 (예: 123-45-67890)

    @Column(name = "ceo_name", length = 100) // 대표자명은 선택사항
    private String ceoName; // 대표자명 (예: 홍길동)

    // === 연락처 정보 필드 ===
    @Column(name = "phone", length = 20) // 전화번호는 선택사항
    private String phone; // 대표 전화번호 (예: 02-1234-5678)

    @Column(name = "fax", length = 20) // 팩스번호는 선택사항
    private String fax; // 팩스번호 (예: 02-1234-5679)

    @Column(name = "email", length = 100) // 이메일은 선택사항
    private String email; // 대표 이메일 (예: contact@company.com)

    // === 주소 및 웹사이트 필드 ===
    @Column(name = "address", length = 500) // 주소는 선택사항
    private String address; // 본사 주소 (예: 서울시 강남구 테헤란로 123)

    @Column(name = "website", length = 200) // 웹사이트는 선택사항
    private String website; // 회사 웹사이트 (예: https://www.company.com)

    // === 회사 상세 정보 필드 ===
    @Column(name = "industry", length = 100) // 업종은 선택사항
    private String industry; // 업종 (예: IT, 제조업, 금융)

    @Lob // Large Object: 긴 텍스트를 저장할 때 사용
    @Column(name = "description", columnDefinition = "TEXT") // PostgreSQL의 TEXT 타입 사용
    // @Lob을 사용하면 최대 길이 제한 없이 긴 텍스트 저장 가능
    private String description; // 회사 설명 (장문의 소개글)

    @Column(name = "employee_count") // 직원 수는 선택사항
    private Integer employeeCount; // 직원 수 (예: 100)

    @Column(name = "established_date", length = 10) // 설립일은 선택사항
    // YYYY-MM-DD 형식의 문자열로 저장 (예: 2020-01-01)
    private String establishedDate; // 설립일

    @Column(name = "logo_url", length = 500) // 로고 URL은 선택사항
    private String logoUrl; // 회사 로고 이미지 URL

    // === Soft Delete 필드 ===
    @Enumerated(EnumType.STRING) // Enum을 문자열로 저장 (ORDINAL은 순서가 바뀌면 문제 발생)
    @Column(name = "del_yn", nullable = false, length = 1) // 삭제 여부는 필수
    @Builder.Default // Builder 사용 시 기본값 설정
    private DelYN delYn = DelYN.N; // 삭제 여부 (N: 정상, Y: 삭제됨)
    // Soft Delete: 실제로 DB에서 삭제하지 않고 delYn을 Y로 변경하여 논리적으로만 삭제

    // === 비즈니스 로직 메서드 ===

    /**
     * 회사 정보 수정
     * 부분 수정 가능 (null이 아닌 값만 업데이트)
     */
    public void updateInfo(
            String name,              // 회사명
            String businessNumber,    // 사업자등록번호
            String ceoName,           // 대표자명
            String phone,             // 전화번호
            String fax,               // 팩스번호
            String email,             // 이메일
            String address,           // 주소
            String website,           // 웹사이트
            String industry,          // 업종
            String description,       // 설명
            Integer employeeCount,    // 직원 수
            String establishedDate,   // 설립일
            String logoUrl            // 로고 URL
    ) {
        // null이 아니고 빈 문자열이 아닌 값만 업데이트 (부분 수정 지원)

        // === 기본 정보 업데이트 ===
        if (name != null && !name.trim().isEmpty()) {
            this.name = name; // 회사명 변경
        }
        if (businessNumber != null && !businessNumber.trim().isEmpty()) {
            this.businessNumber = businessNumber; // 사업자등록번호 변경
        }
        if (ceoName != null && !ceoName.trim().isEmpty()) {
            this.ceoName = ceoName; // 대표자명 변경
        }

        // === 연락처 정보 업데이트 ===
        if (phone != null && !phone.trim().isEmpty()) {
            this.phone = phone; // 전화번호 변경
        }
        if (fax != null && !fax.trim().isEmpty()) {
            this.fax = fax; // 팩스번호 변경
        }
        if (email != null && !email.trim().isEmpty()) {
            this.email = email; // 이메일 변경
        }

        // === 주소 및 웹사이트 업데이트 ===
        if (address != null && !address.trim().isEmpty()) {
            this.address = address; // 주소 변경
        }
        if (website != null && !website.trim().isEmpty()) {
            this.website = website; // 웹사이트 변경
        }

        // === 상세 정보 업데이트 ===
        if (industry != null && !industry.trim().isEmpty()) {
            this.industry = industry; // 업종 변경
        }
        if (description != null && !description.trim().isEmpty()) {
            this.description = description; // 설명 변경
        }
        if (employeeCount != null) {
            this.employeeCount = employeeCount; // 직원 수 변경
        }
        if (establishedDate != null && !establishedDate.trim().isEmpty()) {
            this.establishedDate = establishedDate; // 설립일 변경
        }
        if (logoUrl != null && !logoUrl.trim().isEmpty()) {
            this.logoUrl = logoUrl; // 로고 URL 변경
        }

        // BaseTimeEntity의 updateTimestamp()가 자동으로 updatedAt을 갱신
        // @PreUpdate 라이프사이클 이벤트로 자동 실행
    }

    /**
     * Soft Delete (논리 삭제)
     * 실제로 DB에서 삭제하지 않고 delYn을 Y로 변경
     *
     * Soft Delete를 사용하는 이유:
     * 1. 데이터 복구 가능 (실수로 삭제한 경우)
     * 2. 외래키 참조 무결성 유지 (Member가 Company를 참조)
     * 3. 삭제 이력 추적 (언제, 누가 삭제했는지 확인 가능)
     */
    public void softDelete() {
        this.delYn = DelYN.Y; // 삭제 플래그를 Y로 변경
        // 실제 DELETE 쿼리가 실행되지 않고, UPDATE 쿼리로 delYn만 변경됨
        // SQL: UPDATE company SET del_yn = 'Y', updated_at = NOW() WHERE company_id = ?

        // 이후 조회 시 WHERE delYn = 'N' 조건을 추가하여 삭제된 데이터는 제외
    }

    /**
     * Soft Delete 복구 (재활성화)
     * 삭제된 회사를 다시 활성화
     */
    public void restore() {
        this.delYn = DelYN.N; // 삭제 플래그를 N으로 변경 (복구)
        // 삭제된 회사를 다시 사용 가능한 상태로 복구
    }
}