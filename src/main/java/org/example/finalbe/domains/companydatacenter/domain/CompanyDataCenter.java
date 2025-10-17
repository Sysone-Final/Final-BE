package org.example.finalbe.domains.companydatacenter.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

@Entity
@Table(name = "company_datacenter",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_datacenter", columnNames = {"company_id", "datacenter_id"})
        },
        indexes = {
                @Index(name = "idx_company_id", columnList = "company_id"),
                @Index(name = "idx_datacenter_id", columnList = "datacenter_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CompanyDataCenter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_datacenter_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DataCenter dataCenter;

    @Column(name = "description", length = 500)
    private String description; // 매핑 설명 (예: 계약 내용 등)

    @Column(name = "granted_by", length = 100)
    private String grantedBy; // 권한 부여자

    // 설명 업데이트
    public void updateDescription(String description) {
        this.description = description;
        this.updateTimestamp();
    }
}