package org.example.finalbe.domains.equipment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.*;
import org.example.finalbe.domains.equipment.dto.EquipmentUpdateRequest;
import org.example.finalbe.domains.rack.domain.Rack;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private Long id; // 장비 ID

    @Column(name = "equipment_name", nullable = false, length = 100)
    private String name; // 장비명

    @Column(name = "equipment_code", length = 50)
    private String code; // 장비 코드

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", length = 50)
    private EquipmentType type; // 장비 타입

    @Column(name = "start_unit", nullable = false)
    private Integer startUnit; // 시작 유닛

    @Column(name = "unit_size", nullable = false)
    private Integer unitSize; // 유닛 크기

    @Enumerated(EnumType.STRING)
    @Column(name = "position_type", length = 50)
    private EquipmentPositionType positionType; // 위치 타입

    @Column(name = "model_name", length = 100)
    private String modelName; // 모델명

    @Column(name = "manufacturer", length = 100)
    private String manufacturer; // 제조사

    @Column(name = "serial_number", length = 100)
    private String serialNumber; // 시리얼 번호

    @Column(name = "ip_address", length = 50)
    private String ipAddress; // IP 주소

    @Column(name = "mac_address", length = 50)
    private String macAddress; // MAC 주소

    @Column(name = "os", length = 100)
    private String os; // 운영체제

    @Column(name = "cpu_spec", length = 255)
    private String cpuSpec; // CPU 사양

    @Column(name = "memory_spec", length = 255)
    private String memorySpec; // 메모리 사양

    @Column(name = "disk_spec", length = 255)
    private String diskSpec; // 디스크 사양

    @Column(name = "power_consumption", precision = 10, scale = 2)
    private BigDecimal powerConsumption; // 전력 소비량

    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight; // 무게

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private EquipmentStatus status; // 장비 상태

    @Column(name = "installation_date")
    private LocalDate installationDate; // 설치일

    @Lob
    @Column(name = "notes")
    private String notes; // 비고

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성일시

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정일시

    @Column(name = "maneger_id", nullable = false, length = 50)
    private Long managerId; // 관리자 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id", nullable = false)
    private Rack rack; // 소속 랙

    @Column(name = "position", nullable = false)
    private Integer position; // 위치

    @Column(name = "height", nullable = false)
    private Integer height; // 높이

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false)
    @Builder.Default
    private DelYN delYn = DelYN.N; // 삭제 여부

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.delYn == null) {
            this.delYn = DelYN.N;
        }
        if (this.status == null) {
            this.status = EquipmentStatus.NORMAL;
        }
        if (this.positionType == null) {
            this.positionType = EquipmentPositionType.NORMAL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 타임스탬프 업데이트
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        this.delYn = DelYN.Y;
        this.updateTimestamp();
    }

    /**
     * 장비 정보 수정
     */
    public void updateInfo(EquipmentUpdateRequest request) {
        if (request.equipmentName() != null && !request.equipmentName().trim().isEmpty()) {
            this.name = request.equipmentName();
        }
        if (request.equipmentCode() != null) {
            this.code = request.equipmentCode();
        }
        if (request.equipmentType() != null) {
            this.type = EquipmentType.valueOf(request.equipmentType());
        }
        if (request.positionType() != null) {
            this.positionType = EquipmentPositionType.valueOf(request.positionType());
        }
        if (request.modelName() != null) {
            this.modelName = request.modelName();
        }
        if (request.manufacturer() != null) {
            this.manufacturer = request.manufacturer();
        }
        if (request.serialNumber() != null) {
            this.serialNumber = request.serialNumber();
        }
        if (request.ipAddress() != null) {
            this.ipAddress = request.ipAddress();
        }
        if (request.macAddress() != null) {
            this.macAddress = request.macAddress();
        }
        if (request.os() != null) {
            this.os = request.os();
        }
        if (request.cpuSpec() != null) {
            this.cpuSpec = request.cpuSpec();
        }
        if (request.memorySpec() != null) {
            this.memorySpec = request.memorySpec();
        }
        if (request.diskSpec() != null) {
            this.diskSpec = request.diskSpec();
        }
        if (request.powerConsumption() != null) {
            this.powerConsumption = request.powerConsumption();
        }
        if (request.weight() != null) {
            this.weight = request.weight();
        }
        if (request.installationDate() != null) {
            this.installationDate = request.installationDate();
        }
        if (request.notes() != null) {
            this.notes = request.notes();
        }

        this.updateTimestamp();
    }

    /**
     * 장비 상태 변경
     */
    public void changeStatus(EquipmentStatus newStatus, String reason, String updatedBy) {
        String statusChangeLog = String.format(
                "[%s] 상태 변경: %s → %s (변경자: %s, 사유: %s)",
                LocalDateTime.now(),
                this.status != null ? this.status.name() : "UNKNOWN",
                newStatus.name(),
                updatedBy,
                reason != null ? reason : "없음"
        );

        this.status = newStatus;

        if (this.notes == null || this.notes.trim().isEmpty()) {
            this.notes = statusChangeLog;
        } else {
            this.notes = this.notes + "\n" + statusChangeLog;
        }

        this.updateTimestamp();
    }
}