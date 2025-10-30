package org.example.finalbe.domains.department.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.member.domain.Member;

/**
 * 회원-부서 연결 엔티티 (중간 테이블)
 */
@Entity
@Table(name = "member_department",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_member_department",
                        columnNames = {"member_id", "department_id"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDepartment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_department_id")
    private Long id; // 매핑 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 회원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department; // 부서

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false; // 주 부서 여부

    @Column(name = "position", length = 100)
    private String position; // 직급

    @Column(name = "join_date")
    private java.time.LocalDate joinDate; // 배치일

    @Column(name = "created_by", length = 100)
    private String createdBy; // 생성자

    /**
     * 주 부서로 설정
     */
    public void setPrimaryDepartment() {
        this.isPrimary = true;
    }

    /**
     * 부 부서로 설정
     */
    public void setSecondaryDepartment() {
        this.isPrimary = false;
    }
}