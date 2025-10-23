package org.example.finalbe.domains.department.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.company.domain.Company;

/**
 * 부서 엔티티
 * 회사별 부서 정보 관리
 */
@Entity
@Table(name = "department",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_code_company",
                        columnNames = {"department_code", "company_id"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    @Column(name = "department_code", nullable = false, length = 50)
    private String departmentCode;  // 부서 코드 (예: DEV, HR, SALES)

    @Column(name = "department_name", nullable = false, length = 100)
    private String departmentName;  // 부서명 (예: 개발팀, 인사팀, 영업팀)

    @Column(name = "description", length = 500)
    private String description;  // 부서 설명

    @Column(name = "parent_department_id")
    private Long parentDepartmentId;  // 상위 부서 ID (계층 구조 지원)

    @Column(name = "manager_id")
    private Long managerId;  // 부서장 ID (Member 참조)

    @Column(name = "location", length = 200)
    private String location;  // 부서 위치

    @Column(name = "phone", length = 20)
    private String phone;  // 부서 전화번호

    @Column(name = "email", length = 100)
    private String email;  // 부서 이메일

    @Column(name = "employee_count")
    @Builder.Default
    private Integer employeeCount = 0;  // 소속 직원 수

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false, length = 1)
    @Builder.Default
    private DelYN delYn = DelYN.N;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * 부서 정보 수정
     */
    public void updateInfo(String departmentName, String description, String location,
                           String phone, String email, Long managerId, String updatedBy) {
        if (departmentName != null) this.departmentName = departmentName;
        if (description != null) this.description = description;
        if (location != null) this.location = location;
        if (phone != null) this.phone = phone;
        if (email != null) this.email = email;
        if (managerId != null) this.managerId = managerId;
        this.updatedBy = updatedBy;
    }

    /**
     * 직원 수 증가
     */
    public void incrementEmployeeCount() {
        this.employeeCount++;
    }

    /**
     * 직원 수 감소
     */
    public void decrementEmployeeCount() {
        if (this.employeeCount > 0) {
            this.employeeCount--;
        }
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.delYn = DelYN.Y;
    }
}