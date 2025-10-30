package org.example.finalbe.domains.department.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.member.domain.Member;

/**
 * 회원-부서 연결 엔티티 (Many-to-Many 중간 테이블)
 *
 * - Member와 Department 간의 다대다 관계 표현
 * - 한 회원이 여러 부서에 속할 수 있음 (겸직)
 * - 한 부서에 여러 회원이 속할 수 있음
 * - 주 부서/부 부서 구분 (isPrimary)
 * - 부서별 직급 및 배치일 관리
 *
 * 중간 테이블 설계 이유:
 * 1. 다대다 관계 해소 (Member ↔ Department)
 * 2. 추가 속성 관리 (직급, 배치일, 주 부서 여부)
 * 3. 복합 유니크 제약으로 중복 배치 방지
 * 4. 히스토리 추적 (BaseTimeEntity 상속)
 */
@Entity // JPA 엔티티로 선언 (member_department 테이블과 매핑)
@Table(name = "member_department", // 실제 DB 테이블명 지정
        uniqueConstraints = { // 유니크 제약조건 설정
                @UniqueConstraint(
                        name = "uk_member_department", // 제약조건 이름
                        columnNames = {"member_id", "department_id"} // 복합 유니크 키
                        // 같은 회원이 동일 부서에 중복으로 배치되지 않도록 방지
                        // 예: (회원A, 부서1) OK / (회원A, 부서1) 중복 불가
                )
        }
)
@Getter // 모든 필드에 대한 Getter 자동 생성 (Lombok)
@NoArgsConstructor // 파라미터 없는 기본 생성자 생성 (JPA 스펙 요구사항)
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 생성
@Builder // 빌더 패턴 지원 (가독성 높은 객체 생성)
public class MemberDepartment extends BaseTimeEntity { // BaseTimeEntity 상속 (createdAt, updatedAt 자동 관리)

    // === 식별자 ===
    @Id // Primary Key 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment 전략 (DB에서 자동 생성)
    @Column(name = "member_department_id") // 실제 DB 컬럼명 지정
    private Long id; // 회원-부서 연결 고유 식별자 (예: 1, 2, 3...)

    // === 연관관계 (Member) ===
    @ManyToOne(fetch = FetchType.LAZY) // N:1 관계, 지연 로딩 (실제 사용 시에만 조회)
    @JoinColumn(name = "member_id", nullable = false) // 외래키 컬럼명, NOT NULL 제약
    private Member member; // 연결된 회원 (한 연결은 하나의 회원에만 속함)
    // LAZY 로딩: memberDepartment.getMember().getName() 호출 시점에 SELECT 쿼리 실행
    // 목록 조회 시 불필요한 회원 정보 조회 방지 (N+1 문제 회피)

    // === 연관관계 (Department) ===
    @ManyToOne(fetch = FetchType.LAZY) // N:1 관계, 지연 로딩 (실제 사용 시에만 조회)
    @JoinColumn(name = "department_id", nullable = false) // 외래키 컬럼명, NOT NULL 제약
    private Department department; // 연결된 부서 (한 연결은 하나의 부서에만 속함)
    // LAZY 로딩: memberDepartment.getDepartment().getName() 호출 시점에 SELECT 쿼리 실행
    // 목록 조회 시 불필요한 부서 정보 조회 방지 (N+1 문제 회피)

    // === 주 부서 여부 ===
    @Column(name = "is_primary") // NULL 허용 (기본값 설정)
    @Builder.Default // 빌더 패턴 사용 시에도 기본값 false 적용
    private Boolean isPrimary = false;  // 주 부서 여부 (true: 주 부서, false: 부 부서)
    // 회원은 여러 부서에 속할 수 있지만, 주 부서는 1개만 가능
    // 예: 개발팀(주), 기획팀(부), 마케팅팀(부)

    // === 부서 내 직급 ===
    @Column(name = "position", length = 100) // NULL 허용, 최대 100자
    private String position;  // 해당 부서에서의 직급 (예: 팀장, 매니저, 사원)
    // 같은 회원이라도 부서마다 다른 직급을 가질 수 있음
    // 예: 개발팀에서는 "시니어 개발자", 기획팀에서는 "기획 매니저"

    // === 부서 배치일 ===
    @Column(name = "join_date") // NULL 허용
    private java.time.LocalDate joinDate;  // 부서 배치일 (예: 2023-01-15)
    // 언제부터 해당 부서에 속했는지 기록
    // 부서 이동 히스토리 추적 가능

    // === 생성자 정보 ===
    @Column(name = "created_by", length = 100) // NULL 허용, 최대 100자
    private String createdBy; // 생성자 (예: "admin", "user@company.com")
    // 누가 부서 배치를 생성했는지 추적

    /**
     * 주 부서로 설정
     *
     * - isPrimary를 true로 변경
     * - 회원의 주 부서로 지정
     * - 기존 주 부서는 Service에서 false로 변경 필요
     */
    public void setPrimaryDepartment() {
        this.isPrimary = true; // 주 부서 플래그를 true로 설정
        // JPA Dirty Checking으로 UPDATE member_department SET is_primary = true WHERE member_department_id = ?
        // 주의: 기존 주 부서를 false로 변경하는 로직은 Service에서 처리 필요
    }

    /**
     * 부 부서로 설정
     *
     * - isPrimary를 false로 변경
     * - 회원의 부 부서로 지정
     */
    public void setSecondaryDepartment() {
        this.isPrimary = false; // 주 부서 플래그를 false로 설정
        // JPA Dirty Checking으로 UPDATE member_department SET is_primary = false WHERE member_department_id = ?
    }
}