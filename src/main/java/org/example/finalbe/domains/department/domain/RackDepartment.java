package org.example.finalbe.domains.department.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.rack.domain.Rack;

/**
 * 랙-부서 연결 엔티티 (중간 테이블)
 */
@Entity
@Table(name = "rack_department",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rack_department",
                        columnNames = {"rack_id", "department_id"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RackDepartment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rack_department_id")
    private Long id; // 매핑 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id", nullable = false)
    private Rack rack; // 랙

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department; // 부서

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false; // 주 담당 부서 여부

    @Column(name = "responsibility", length = 200)
    private String responsibility; // 담당 업무

    @Column(name = "assigned_date")
    private java.time.LocalDate assignedDate; // 배정일

    @Column(name = "created_by", length = 100)
    private String createdBy; // 생성자

    /**
     * 주 담당 부서로 설정
     */
    public void setPrimaryDepartment() {
        this.isPrimary = true;
    }

    /**
     * 부 담당 부서로 설정
     */
    public void setSecondaryDepartment() {
        this.isPrimary = false;
    }
}