package org.example.finalbe.domains.equipment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.*;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 장비 엔티티
 */
@Entity
@Table(name = "equipment", indexes = {
        @Index(name = "idx_equipment_company_id", columnList = "company_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Equipment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_id")
    private Long id;

    // ========== 회사 정보 ==========
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "equipment_name", nullable = false, length = 100)
    private String name;

    @Column(name = "equipment_code", length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", length = 50)
    private EquipmentType type;

    // 랙을 선택적으로 설정할 수 있도록 nullable = true로 변경
    @Column(name = "start_unit", nullable = true)
    private Integer startUnit;

    @Column(name = "unit_size", nullable = true)
    private Integer unitSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_type", length = 50, nullable = true)
    private EquipmentPositionType positionType;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "mac_address", length = 50)
    private String macAddress;

    @Column(name = "os", length = 100)
    private String os;

    @Column(name = "cpu_spec", length = 200)
    private String cpuSpec;

    @Column(name = "memory_spec", length = 200)
    private String memorySpec;

    @Column(name = "disk_spec", length = 200)
    private String diskSpec;

    @Column(name = "power_consumption", precision = 10, scale = 2)
    private BigDecimal powerConsumption;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EquipmentStatus status;

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    // ========== 모니터링 설정 필드 ==========
    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled;

    @Column(name = "cpu_threshold_warning")
    private Integer cpuThresholdWarning;

    @Column(name = "cpu_threshold_critical")
    private Integer cpuThresholdCritical;

    @Column(name = "memory_threshold_warning")
    private Integer memoryThresholdWarning;

    @Column(name = "memory_threshold_critical")
    private Integer memoryThresholdCritical;

    @Column(name = "disk_threshold_warning")
    private Integer diskThresholdWarning;

    @Column(name = "disk_threshold_critical")
    private Integer diskThresholdCritical;

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false, length = 1)
    @Builder.Default
    private DelYN delYn = DelYN.N;

    // 랙을 선택적으로 설정할 수 있도록 nullable = true로 변경
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id", nullable = true)
    private Rack rack;

    /**
     * 장비 정보 수정
     */
    public void updateInfo(
            String name,
            String code,
            EquipmentType type,
            String modelName,
            String manufacturer,
            String serialNumber,
            Integer startUnit,
            String ipAddress,
            String macAddress,
            String os,
            String cpuSpec,
            String memorySpec,
            String diskSpec,
            BigDecimal powerConsumption,
            EquipmentStatus status,
            LocalDate installationDate,
            String notes,
            Boolean monitoringEnabled,
            Integer cpuThresholdWarning,
            Integer cpuThresholdCritical,
            Integer memoryThresholdWarning,
            Integer memoryThresholdCritical,
            Integer diskThresholdWarning,
            Integer diskThresholdCritical
    ) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.modelName = modelName;
        this.manufacturer = manufacturer;
        this.serialNumber = serialNumber;
        this.startUnit = startUnit;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.os = os;
        this.cpuSpec = cpuSpec;
        this.memorySpec = memorySpec;
        this.diskSpec = diskSpec;
        this.powerConsumption = powerConsumption;
        this.status = status;
        this.installationDate = installationDate;
        this.notes = notes;
        this.monitoringEnabled = monitoringEnabled;
        this.cpuThresholdWarning = cpuThresholdWarning;
        this.cpuThresholdCritical = cpuThresholdCritical;
        this.memoryThresholdWarning = memoryThresholdWarning;
        this.memoryThresholdCritical = memoryThresholdCritical;
        this.diskThresholdWarning = diskThresholdWarning;
        this.diskThresholdCritical = diskThresholdCritical;
    }

    /**
     * 논리 삭제
     */
    public void softDelete() {
        this.delYn = DelYN.Y;
    }

    /**
     * 랙 ID 조회 (Transient 메서드)
     */
    @Transient
    public Long getRackId() {
        return rack != null ? rack.getId() : null;
    }

}