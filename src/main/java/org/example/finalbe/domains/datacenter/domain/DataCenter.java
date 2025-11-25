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
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", length = 1)
    @Builder.Default
    private DelYN delYn = DelYN.N;

    // ✅ 알림 모니터링 활성화 여부
    @Column(name = "monitoring_enabled")
    @Builder.Default
    private Boolean monitoringEnabled = true;

    // ✅ CPU 평균 임계치 (WARNING)
    @Column(name = "avg_cpu_threshold_warning")
    private Integer avgCpuThresholdWarning;

    // ✅ CPU 평균 임계치 (CRITICAL)
    @Column(name = "avg_cpu_threshold_critical")
    private Integer avgCpuThresholdCritical;

    // ✅ 메모리 평균 임계치 (WARNING)
    @Column(name = "avg_memory_threshold_warning")
    private Integer avgMemoryThresholdWarning;

    // ✅ 메모리 평균 임계치 (CRITICAL)
    @Column(name = "avg_memory_threshold_critical")
    private Integer avgMemoryThresholdCritical;

    // ✅ 디스크 평균 임계치 (WARNING)
    @Column(name = "avg_disk_threshold_warning")
    private Integer avgDiskThresholdWarning;

    // ✅ 디스크 평균 임계치 (CRITICAL)
    @Column(name = "avg_disk_threshold_critical")
    private Integer avgDiskThresholdCritical;

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
     * ✅ 평균 메트릭 임계치 설정
     */
    public void updateAverageThresholds(
            Integer cpuWarning,
            Integer cpuCritical,
            Integer memWarning,
            Integer memCritical,
            Integer diskWarning,
            Integer diskCritical) {
        this.avgCpuThresholdWarning = cpuWarning;
        this.avgCpuThresholdCritical = cpuCritical;
        this.avgMemoryThresholdWarning = memWarning;
        this.avgMemoryThresholdCritical = memCritical;
        this.avgDiskThresholdWarning = diskWarning;
        this.avgDiskThresholdCritical = diskCritical;
    }

    /**
     * ✅ 모니터링 활성화/비활성화 토글
     */
    public void toggleMonitoring() {
        this.monitoringEnabled = !this.monitoringEnabled;
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