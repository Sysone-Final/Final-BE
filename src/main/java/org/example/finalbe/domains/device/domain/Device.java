/**
 * 작성자: 황요한
 * 장치 엔티티
 */
package org.example.finalbe.domains.device.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DeviceStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.rack.domain.Rack;

import java.time.LocalDate;

@Entity
@Table(name = "device",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "rack_id", name = "uk_device_rack")
        })
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Device extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "device_code", length = 50)
    private String deviceCode;

    @Column(name = "gridY")
    private Integer gridY;

    @Column(name = "gridX")
    private Integer gridX;

    @Column(name = "gridZ")
    private Integer gridZ;

    @Column(name = "rotation")
    private Integer rotation;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id", nullable = false)
    private DeviceType deviceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serverroom_id")
    private ServerRoom serverRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id")
    private Rack rack;

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