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
@Table(name = "equipment")
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


    // ========== 모니터링 설정 필드 추가 ==========
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
        if (name != null) this.name = name;
        if (code != null) this.code = code;
        if (type != null) this.type = type;
        if (startUnit != null) this.startUnit = startUnit;
        if (modelName != null) this.modelName = modelName;
        if (manufacturer != null) this.manufacturer = manufacturer;
        if (serialNumber != null) this.serialNumber = serialNumber;
        if (ipAddress != null) this.ipAddress = ipAddress;
        if (macAddress != null) this.macAddress = macAddress;
        if (os != null) this.os = os;
        if (cpuSpec != null) this.cpuSpec = cpuSpec;
        if (memorySpec != null) this.memorySpec = memorySpec;
        if (diskSpec != null) this.diskSpec = diskSpec;
        if (powerConsumption != null) this.powerConsumption = powerConsumption;
        if (status != null) this.status = status;
        if (installationDate != null) this.installationDate = installationDate;
        if (notes != null) this.notes = notes;
        if (monitoringEnabled != null) this.monitoringEnabled = monitoringEnabled;
        if (cpuThresholdWarning != null) this.cpuThresholdWarning = cpuThresholdWarning;
        if (cpuThresholdCritical != null) this.cpuThresholdCritical = cpuThresholdCritical;
        if (memoryThresholdWarning != null) this.memoryThresholdWarning = memoryThresholdWarning;
        if (memoryThresholdCritical != null) this.memoryThresholdCritical = memoryThresholdCritical;
        if (diskThresholdWarning != null) this.diskThresholdWarning = diskThresholdWarning;
        if (diskThresholdCritical != null) this.diskThresholdCritical = diskThresholdCritical;

        this.updateTimestamp();
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.delYn = DelYN.Y;
        this.updateTimestamp();
    }

    /**
     * 장비 상태 변경
     */
    public void updateStatus(EquipmentStatus newStatus) {
        this.status = newStatus;
    }
}