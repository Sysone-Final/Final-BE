package org.example.finalbe.domains.department.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.rack.domain.Rack;

/**
 * 랙-부서 연결 엔티티 (Many-to-Many 중간 테이블)
 *
 * - Rack과 Department 간의 다대다 관계 표현
 * - 한 랙이 여러 부서에 의해 관리될 수 있음 (공동 관리)
 * - 한 부서가 여러 랙을 관리할 수 있음
 * - 주 담당 부서/부 담당 부서 구분 (isPrimary)
 * - 부서별 책임 범위 및 배정일 관리
 *
 * 중간 테이블 설계 이유:
 * 1. 다대다 관계 해소 (Rack ↔ Department)
 * 2. 추가 속성 관리 (책임 범위, 배정일, 주 담당 여부)
 * 3. 복합 유니크 제약으로 중복 배정 방지
 * 4. 랙 관리 책임 추적 및 히스토리 관리
 */
@Entity // JPA 엔티티로 선언 (rack_department 테이블과 매핑)
@Table(name = "rack_department", // 실제 DB 테이블명 지정
        uniqueConstraints = { // 유니크 제약조건 설정
                @UniqueConstraint(
                        name = "uk_rack_department", // 제약조건 이름
                        columnNames = {"rack_id", "department_id"} // 복합 유니크 키
                        // 같은 랙이 동일 부서에 중복으로 배정되지 않도록 방지
                        // 예: (랙A, 부서1) OK / (랙A, 부서1) 중복 불가
                )
        }
)
@Getter // 모든 필드에 대한 Getter 자동 생성 (Lombok)
@NoArgsConstructor // 파라미터 없는 기본 생성자 생성 (JPA 스펙 요구사항)
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 생성
@Builder // 빌더 패턴 지원 (가독성 높은 객체 생성)
public class RackDepartment extends BaseTimeEntity { // BaseTimeEntity 상속 (createdAt, updatedAt 자동 관리)

    // === 식별자 ===
    @Id // Primary Key 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment 전략 (DB에서 자동 생성)
    @Column(name = "rack_department_id") // 실제 DB 컬럼명 지정
    private Long id; // 랙-부서 연결 고유 식별자 (예: 1, 2, 3...)

    // === 연관관계 (Rack) ===
    @ManyToOne(fetch = FetchType.LAZY) // N:1 관계, 지연 로딩 (실제 사용 시에만 조회)
    @JoinColumn(name = "rack_id", nullable = false) // 외래키 컬럼명, NOT NULL 제약
    private Rack rack; // 연결된 랙 (한 연결은 하나의 랙에만 속함)
    // LAZY 로딩: rackDepartment.getRack().getRackCode() 호출 시점에 SELECT 쿼리 실행
    // 목록 조회 시 불필요한 랙 정보 조회 방지 (N+1 문제 회피)

    // === 연관관계 (Department) ===
    @ManyToOne(fetch = FetchType.LAZY) // N:1 관계, 지연 로딩 (실제 사용 시에만 조회)
    @JoinColumn(name = "department_id", nullable = false) // 외래키 컬럼명, NOT NULL 제약
    private Department department; // 연결된 부서 (한 연결은 하나의 부서에만 속함)
    // LAZY 로딩: rackDepartment.getDepartment().getName() 호출 시점에 SELECT 쿼리 실행
    // 목록 조회 시 불필요한 부서 정보 조회 방지 (N+1 문제 회피)

    // === 주 담당 부서 여부 ===
    @Column(name = "is_primary") // NULL 허용 (기본값 설정)
    @Builder.Default // 빌더 패턴 사용 시에도 기본값 false 적용
    private Boolean isPrimary = false;  // 주 담당 부서 여부 (true: 주 담당, false: 부 담당)
    // 랙은 여러 부서가 관리할 수 있지만, 주 담당 부서는 1개만 가능
    // 예: 서버실 랙 → 개발팀(주 담당), 인프라팀(부 담당)

    // === 책임 범위 ===
    @Column(name = "responsibility", length = 200) // NULL 허용, 최대 200자
    private String responsibility;  // 담당 업무/책임 범위 (예: 일상 점검 및 유지보수, 장애 대응)
    // 해당 부서가 이 랙에 대해 어떤 업무를 담당하는지 명시
    // 예: "하드웨어 유지보수", "네트워크 모니터링", "보안 점검"

    // === 배정일 ===
    @Column(name = "assigned_date") // NULL 허용
    private java.time.LocalDate assignedDate;  // 배정일 (예: 2023-01-15)
    // 언제부터 해당 부서가 이 랙을 담당했는지 기록
    // 담당 부서 변경 히스토리 추적 가능

    // === 생성자 정보 ===
    @Column(name = "created_by", length = 100) // NULL 허용, 최대 100자
    private String createdBy; // 생성자 (예: "admin", "user@company.com")
    // 누가 랙 배정을 생성했는지 추적

    /**
     * 주 담당 부서로 설정
     *
     * - isPrimary를 true로 변경
     * - 랙의 주 담당 부서로 지정
     * - 기존 주 담당 부서는 Service에서 false로 변경 필요
     */
    public void setPrimaryDepartment() {
        this.isPrimary = true; // 주 담당 플래그를 true로 설정
        // JPA Dirty Checking으로 UPDATE rack_department SET is_primary = true WHERE rack_department_id = ?
        // 주의: 기존 주 담당 부서를 false로 변경하는 로직은 Service에서 처리 필요
    }

    /**
     * 부 담당 부서로 설정
     *
     * - isPrimary를 false로 변경
     * - 랙의 부 담당 부서로 지정
     */
    public void setSecondaryDepartment() {
        this.isPrimary = false; // 주 담당 플래그를 false로 설정
        // JPA Dirty Checking으로 UPDATE rack_department SET is_primary = false WHERE rack_department_id = ?
    }
}