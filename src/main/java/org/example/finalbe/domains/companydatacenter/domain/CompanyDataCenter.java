package org.example.finalbe.domains.companydatacenter.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

/**
 * 회사-전산실 매핑 엔티티
 * 회사와 전산실 간의 N:M 관계를 관리하는 중간 테이블
 * 어떤 회사가 어떤 전산실에 접근 가능한지 권한 관리
 */
@Entity // JPA 엔티티 선언
@Table(name = "company_datacenter", // 실제 테이블 이름
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_datacenter",
                        columnNames = {"company_id", "datacenter_id"})
                // 동일 회사-전산실 조합 중복 방지
        },
        indexes = {
                @Index(name = "idx_company_id", columnList = "company_id"),
                // 회사별 조회 성능 최적화
                @Index(name = "idx_datacenter_id", columnList = "datacenter_id")
                // 전산실별 조회 성능 최적화
        }
)
@NoArgsConstructor // JPA 기본 생성자
@AllArgsConstructor // 모든 필드 생성자
@Getter // getter 자동 생성
@Builder // 빌더 패턴
public class CompanyDataCenter extends BaseTimeEntity { // 생성/수정 시간 자동 관리

    @Id // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment
    @Column(name = "company_datacenter_id")
    private Long id; // 매핑 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩
    @JoinColumn(name = "company_id", nullable = false) // 외래키
    private Company company; // 매핑된 회사

    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩
    @JoinColumn(name = "datacenter_id", nullable = false) // 외래키
    private DataCenter dataCenter; // 매핑된 전산실

    @Column(name = "description", length = 500)
    private String description; // 매핑 설명 (예: 계약 내용)

    @Column(name = "granted_by", length = 100)
    private String grantedBy; // 권한 부여자 (예: admin)

    /**
     * 매핑 설명 업데이트
     */
    public void updateDescription(String description) {
        this.description = description; // 설명 변경
        this.updateTimestamp(); // updated_at 갱신
        // JPA Dirty Checking으로 UPDATE 쿼리 자동 실행
    }
}