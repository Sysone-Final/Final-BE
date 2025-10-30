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
 */
@Entity
@Table(name = "device")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Device extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id; // 장치 ID

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName; // 장치명

    @Column(name = "device_code", length = 50)
    private String deviceCode; // 장치 코드

    @Column(name = "gridY")
    private Integer gridY; // Y축 위치 (행)

    @Column(name = "gridX")
    private Integer gridX; // X축 위치 (열)

    @Column(name = "gridZ")
    private Integer gridZ; // Z축 위치

    @Column(name = "rotation")
    private Integer rotation; // 회전 각도 (0, 90, 180, 270)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private DeviceStatus status; // 장치 상태

    @Column(name = "model_name", length = 100)
    private String modelName; // 모델명

    @Column(name = "manufacturer", length = 100)
    private String manufacturer; // 제조사

    @Column(name = "serial_number", length = 100)
    private String serialNumber; // 시리얼 번호

    @Column(name = "purchase_date")
    private LocalDate purchaseDate; // 구매일

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate; // 보증 종료일

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // 비고

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id", nullable = false)
    private DeviceType deviceType; // 장치 타입

    @Column(name = "manager_id", nullable = false)
    private Long managerId; // 관리자 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DataCenter datacenter; // 소속 전산실

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id")
    private Rack rack; // 소속 랙 (server 타입일 경우)

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
     * 장치 위치 변경
     */
    public void updatePosition(Integer gridY, Integer gridX, Integer gridZ, Integer rotation) {
        this.gridY = gridY;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.rotation = rotation;
    }
}