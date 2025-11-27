/**
 * 작성자: 황요한
 * 데이터센터 엔티티
 * - 회사 소속 데이터센터 정보 관리
 * - 모니터링 활성 여부 및 평균 임계치 설정 포함
 */
package org.example.finalbe.domains.datacenter.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.company.domain.Company;

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

    /** 데이터센터 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "datacenter_id")
    private Long id;

    /** 데이터센터 코드 */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** 데이터센터 이름 */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** 주소 */
    @Column(name = "address", length = 500)
    private String address;

    /** 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 소속 회사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    /** 삭제 여부 */
    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", length = 1)
    @Builder.Default
    private DelYN delYn = DelYN.N;

    /** 모니터링 활성화 여부 */
    @Column(name = "monitoring_enabled")
    @Builder.Default
    private Boolean monitoringEnabled = true;

    /** 평균 CPU 임계치 - WARNING */
    @Column(name = "avg_cpu_threshold_warning")
    private Integer avgCpuThresholdWarning;

    /** 평균 CPU 임계치 - CRITICAL */
    @Column(name = "avg_cpu_threshold_critical")
    private Integer avgCpuThresholdCritical;

    /** 평균 메모리 임계치 - WARNING */
    @Column(name = "avg_memory_threshold_warning")
    private Integer avgMemoryThresholdWarning;

    /** 평균 메모리 임계치 - CRITICAL */
    @Column(name = "avg_memory_threshold_critical")
    private Integer avgMemoryThresholdCritical;

    /** 평균 디스크 임계치 - WARNING */
    @Column(name = "avg_disk_threshold_warning")
    private Integer avgDiskThresholdWarning;

    /** 평균 디스크 임계치 - CRITICAL */
    @Column(name = "avg_disk_threshold_critical")
    private Integer avgDiskThresholdCritical;

    /**
     * 데이터센터 기본 정보 수정
     */
    public void updateInfo(String name, String address, String description) {
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
     * 평균 임계치 설정
     */
    public void updateAverageThresholds(
            Integer cpuWarning, Integer cpuCritical,
            Integer memWarning, Integer memCritical,
            Integer diskWarning, Integer diskCritical
    ) {
        this.avgCpuThresholdWarning = cpuWarning;
        this.avgCpuThresholdCritical = cpuCritical;
        this.avgMemoryThresholdWarning = memWarning;
        this.avgMemoryThresholdCritical = memCritical;
        this.avgDiskThresholdWarning = diskWarning;
        this.avgDiskThresholdCritical = diskCritical;
    }

    /**
     * 모니터링 활성/비활성 토글
     */
    public void toggleMonitoring() {
        this.monitoringEnabled = !this.monitoringEnabled;
    }

    /**
     * 소속 회사 변경
     */
    public void setCompany(Company company) {
        this.company = company;
    }

    /**
     * 소프트 삭제 처리
     */
    public void softDelete() {
        this.delYn = DelYN.Y;
    }
}
