package org.example.finalbe.domains.companyserverroom.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

/**
 * 회사-서버실 매핑 엔티티
 * (기존 회사-전산실 매핑)
 */
@Entity
@Table(name = "company_serverroom",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_serverroom",
                        columnNames = {"company_id", "serverroom_id"})
        },
        indexes = {
                @Index(name = "idx_company_serverroom_company_id", columnList = "company_id"),
                @Index(name = "idx_company_serverroom_serverroom_id", columnList = "serverroom_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CompanyServerRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_serverroom_id")
    private Long id; // 매핑 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // 소속 회사

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serverroom_id", nullable = false)
    private ServerRoom serverRoom; // 접근 가능한 서버실

    @Column(name = "description", length = 500)
    private String description; // 매핑 설명

    @Column(name = "granted_by", length = 100)
    private String grantedBy; // 권한 부여자
}