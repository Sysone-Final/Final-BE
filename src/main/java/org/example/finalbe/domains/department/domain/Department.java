package org.example.finalbe.domains.department.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.company.domain.Company;

/**
 * 부서 엔티티 (Entity)
 *
 * - 회사 내 조직 구조를 표현하는 핵심 엔티티
 * - Company와 N:1 관계 (한 회사는 여러 부서 보유)
 * - Unique 제약: 같은 회사 내에서 부서 코드 중복 불가
 * - Soft Delete 지원 (delYn 컬럼으로 논리 삭제)
 *
 * 엔티티 설계 이유:
 * 1. 부서별 직원 수 추적 (employeeCount)
 * 2. 부서별 연락처 및 위치 정보 관리
 * 3. 회사별 부서 분리 및 조회 최적화
 * 4. Soft Delete로 히스토리 보존
 */
@Entity // JPA 엔티티로 선언 (department 테이블과 매핑)
@Table(name = "department", // 실제 DB 테이블명 지정
        uniqueConstraints = { // 유니크 제약조건 설정
                @UniqueConstraint(
                        name = "uk_department_code_company", // 제약조건 이름
                        columnNames = {"department_code", "company_id"} // 복합 유니크 키
                        // 같은 회사 내에서 동일한 부서 코드 중복 방지
                        // 예: (회사A, "DEV"), (회사A, "HR") OK / (회사A, "DEV") 중복 불가
                )
        }
)
@Getter // 모든 필드에 대한 Getter 자동 생성 (Lombok)
@NoArgsConstructor // 파라미터 없는 기본 생성자 생성 (JPA 스펙 요구사항)
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 생성
@Builder // 빌더 패턴 지원 (가독성 높은 객체 생성)
public class Department extends BaseTimeEntity { // BaseTimeEntity 상속 (createdAt, updatedAt 자동 관리)

    // === 식별자 ===
    @Id // Primary Key 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment 전략 (DB에서 자동 생성)
    @Column(name = "department_id") // 실제 DB 컬럼명 지정
    private Long id; // 부서 고유 식별자 (예: 1, 2, 3...)

    // === 부서 기본 정보 ===
    @Column(name = "department_code", nullable = false, length = 50) // NOT NULL 제약, 최대 50자
    private String departmentCode;  // 부서 코드 (예: DEV, HR, SALES)
    // 회사 내에서 부서를 구분하는 짧은 코드

    @Column(name = "department_name", nullable = false, length = 100) // NOT NULL 제약, 최대 100자
    private String departmentName;  // 부서명 (예: 개발팀, 인사팀, 영업팀)
    // 실제 표시되는 부서 이름

    @Column(name = "description", length = 500) // NULL 허용, 최대 500자
    private String description;  // 부서 설명 (예: 서버 개발 및 유지보수 담당)
    // 부서의 역할 및 업무 범위 설명

    // === 부서 위치 및 연락처 ===
    @Column(name = "location", length = 200) // NULL 허용, 최대 200자
    private String location;  // 부서 위치 (예: 본사 3층, 서울 지사 2층)
    // 물리적 사무실 위치 정보

    @Column(name = "phone", length = 20) // NULL 허용, 최대 20자
    private String phone;  // 부서 전화번호 (예: 02-1234-5678)
    // 부서 대표 전화번호

    @Column(name = "email", length = 100) // NULL 허용, 최대 100자
    private String email;  // 부서 이메일 (예: dev@company.com)
    // 부서 공용 이메일 주소

    // === 부서 통계 정보 ===
    @Column(name = "employee_count") // NULL 허용 (기본값 설정)
    @Builder.Default // 빌더 패턴 사용 시에도 기본값 0 적용
    private Integer employeeCount = 0;  // 소속 직원 수 (예: 5, 10, 15...)
    // MemberDepartment 관계 추가/삭제 시 자동 증감
    // 집계 함수 대신 역정규화로 조회 성능 최적화

    // === 삭제 여부 (Soft Delete) ===
    @Enumerated(EnumType.STRING) // Enum을 문자열로 저장 (Y, N)
    @Column(name = "del_yn", nullable = false, length = 1) // NOT NULL 제약, 최대 1자
    @Builder.Default // 빌더 패턴 사용 시에도 기본값 N 적용
    private DelYN delYn = DelYN.N; // 삭제 여부 (N: 정상, Y: 삭제됨)
    // 물리 삭제 대신 논리 삭제 (히스토리 보존 및 복구 가능)

    // === 생성/수정자 정보 ===
    @Column(name = "created_by", length = 100) // NULL 허용, 최대 100자
    private String createdBy; // 생성자 (예: "admin", "user@company.com")
    // 누가 부서를 생성했는지 추적

    @Column(name = "updated_by", length = 100) // NULL 허용, 최대 100자
    private String updatedBy; // 수정자 (예: "admin", "user@company.com")
    // 누가 마지막으로 부서를 수정했는지 추적

    // === 연관관계 ===
    @ManyToOne(fetch = FetchType.LAZY) // N:1 관계, 지연 로딩 (실제 사용 시에만 조회)
    @JoinColumn(name = "company_id", nullable = false) // 외래키 컬럼명, NOT NULL 제약
    private Company company; // 소속 회사 (한 부서는 하나의 회사에만 속함)
    // LAZY 로딩: department.getCompany().getName() 호출 시점에 SELECT 쿼리 실행
    // 부서 목록 조회 시 불필요한 회사 정보 조회 방지 (N+1 문제 회피)

    /**
     * 부서 정보 수정
     *
     * - 변경 가능 필드: 부서명, 설명, 위치, 전화번호, 이메일
     * - null이 아닌 값만 업데이트 (부분 수정 지원)
     * - 수정자 정보 자동 기록
     *
     * @param departmentName 부서명 (null이면 변경 안 함)
     * @param description 설명 (null이면 변경 안 함)
     * @param location 위치 (null이면 변경 안 함)
     * @param phone 전화번호 (null이면 변경 안 함)
     * @param email 이메일 (null이면 변경 안 함)
     * @param updatedBy 수정자 정보 (필수)
     */
    public void updateInfo(String departmentName, String description, String location,
                           String phone, String email, String updatedBy) {
        // === null 체크 후 부분 수정 (Dirty Checking) ===
        if (departmentName != null) this.departmentName = departmentName; // 부서명이 전달되면 업데이트
        if (description != null) this.description = description; // 설명이 전달되면 업데이트
        if (location != null) this.location = location; // 위치가 전달되면 업데이트
        if (phone != null) this.phone = phone; // 전화번호가 전달되면 업데이트
        if (email != null) this.email = email; // 이메일이 전달되면 업데이트
        this.updatedBy = updatedBy; // 수정자 정보는 항상 업데이트
        // JPA Dirty Checking: 트랜잭션 커밋 시 변경된 필드만 UPDATE 쿼리 자동 생성
        // 예: UPDATE department SET department_name = ?, updated_by = ?, updated_at = ? WHERE department_id = ?
    }

    /**
     * 직원 수 증가
     *
     * - MemberDepartment 생성 시 호출
     * - employeeCount를 1 증가시킴
     */
    public void incrementEmployeeCount() {
        this.employeeCount++; // 직원 수 +1
        // 예: 5명 → 6명
        // JPA Dirty Checking으로 UPDATE department SET employee_count = 6 WHERE department_id = ?
    }

    /**
     * 직원 수 감소
     *
     * - MemberDepartment 삭제 시 호출
     * - employeeCount를 1 감소시킴 (0 미만으로 내려가지 않음)
     */
    public void decrementEmployeeCount() {
        if (this.employeeCount > 0) { // 음수 방지 (방어적 프로그래밍)
            this.employeeCount--; // 직원 수 -1
            // 예: 6명 → 5명
            // JPA Dirty Checking으로 UPDATE department SET employee_count = 5 WHERE department_id = ?
        }
        // 이미 0이면 아무 동작 안 함 (데이터 무결성 보호)
    }

    /**
     * 소프트 삭제 (Soft Delete)
     *
     * - 물리 삭제 대신 delYn을 Y로 변경
     * - 데이터는 DB에 남아있지만 조회 시 제외됨
     * - 히스토리 보존 및 복구 가능
     */
    public void softDelete() {
        this.delYn = DelYN.Y; // 삭제 플래그를 Y로 변경
        // JPA Dirty Checking으로 UPDATE department SET del_yn = 'Y' WHERE department_id = ?
        // Repository 조회 시 WHERE del_yn = 'N' 조건 추가로 자동 필터링
    }
}