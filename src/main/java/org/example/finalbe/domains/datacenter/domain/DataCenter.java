// src/main/java/org/example/finalbe/domains/datacenter/domain/DataCenter.java

package org.example.finalbe.domains.datacenter.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.company.domain.Company;

/**
 * 데이터센터 엔티티
 * 서버실을 그룹화하기 위한 목적
 */
@Entity
@Table(name = "datacenter",
        indexes = {
                @Index(name = "idx_datacenter_name", columnList = "name"),
                @Index(name = "idx_datacenter_code", columnList = "code"),
                @Index(name = "idx_datacenter_company", columnList = "company_id")
        })
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class DataCenter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "datacenter_id")
    private Long id; // 데이터센터 ID

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // 데이터센터 코드

    @Column(name = "name", nullable = false, length = 200)
    private String name; // 데이터센터명

    @Column(name = "address", length = 500)
    private String address; // 주소

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 설명

    // 회사 연관관계 추가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company; // 소속 회사

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", length = 1)
    @Builder.Default
    private DelYN delYn = DelYN.N; // 삭제 여부

    /**
     * 데이터센터 정보 수정
     */
    public void updateInfo(
            String name,
            String address,
            String description
    ) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
        if (description != null) {
            this.description = description;
        }
    }

    /**
     * 회사 설정
     */
    public void setCompany(Company company) {
        this.company = company;
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.delYn = DelYN.Y;
    }
}