/**
 * 작성자: 황요한
 * 회사-서버실 매핑 엔티티
 */
package org.example.finalbe.domains.companyserverroom.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serverroom_id", nullable = false)
    private ServerRoom serverRoom;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "granted_by", length = 100)
    private String grantedBy;
}