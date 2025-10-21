package org.example.finalbe.domains.device.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DeviceStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.rack.domain.Rack;

import java.time.LocalDate;

/**
 * 장치 엔티티
 * 전산실에 배치되는 물리적 장치 (server, door, aircon 등)
 */
@Entity
@Table(name = "device")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @Column(name = "deivce_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "device_code", length = 50)
    private String deviceCode;

    // 그리드 좌표 (3D 배치)
    @Column(name = "gridY")
    private Integer gridY;

    @Column(name = "gridX") //position_col
    private Integer gridX;

    @Column(name = "gridZ")
    private Integer gridZ;

    @Column(name = "rotation")
    private Integer rotation;     // 회전 각도 (0, 90, 180, 270)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private DeviceStatus status;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id", nullable = false)
    private DeviceType deviceType;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DataCenter datacenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id")
    private Rack rack;  // server 타입일 경우 rack 연결

    /**
     * 장치 상태 변경
     */
    public void changeStatus(DeviceStatus newStatus, String reason) {
        this.status = newStatus;
        if (reason != null && !reason.trim().isEmpty()) {
            this.notes = (this.notes != null ? this.notes + "\n" : "")
                    + "[" + java.time.LocalDateTime.now() + "] 상태 변경: "
                    + this.status + " -> " + newStatus + " (사유: " + reason + ")";
        }
    }

    /**
     * 위치 업데이트
     */
    public void updatePosition(Integer gridX, Integer gridY, Integer gridZ, Integer rotation) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.gridZ = gridZ;
        this.rotation = rotation;
    }
}