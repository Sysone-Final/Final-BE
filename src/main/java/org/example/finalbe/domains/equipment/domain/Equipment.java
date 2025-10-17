package org.example.finalbe.domains.equipment.domain;

import jakarta.persistence.*;

import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.common.enumdir.PositionType;
import org.example.finalbe.domains.rack.domain.Rack;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "start_unit", nullable = false)
    private Integer startUnit;

    @Column(name = "unit_size", nullable = false)
    private Integer unitSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_type", length = 50)
    private PositionType positionType;

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

    @Column(name = "cpu_spec", length = 255)
    private String cpuSpec;

    @Column(name = "memory_spec", length = 255)
    private String memorySpec;

    @Column(name = "disk_spec", length = 255)
    private String diskSpec;

    @Column(name = "power_consumption")
    private Double powerConsumption; // W

    @Column(name = "weight")
    private Double weight; // kg

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private EquipmentStatus status;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Temporal(TemporalType.DATE)
    @Column(name = "installation_date")
    private Date installationDate;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "maneger_id", nullable = false, length = 50)
    private String managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id", nullable = false)
    private Rack rack;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "height", nullable = false)
    private Integer height;
}
