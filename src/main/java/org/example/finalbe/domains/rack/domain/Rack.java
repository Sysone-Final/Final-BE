package org.example.finalbe.domains.rack.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DoorDirection;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.common.enumdir.RackType;
import org.example.finalbe.domains.common.enumdir.ZoneDirection;
import org.example.finalbe.domains.datacenter.domain.DataCenter;


@Entity
@Table(name = "rack")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rack extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rack_id")
    private Long id;

    @Column(name = "rack_name", nullable = false, length = 100)
    private String rackName;

    @Column(name = "group_number", length = 50)
    private String groupNumber;

    @Column(name = "rack_location", length = 100)
    private String rackLocation;

    @Column(name = "total_units", nullable = false)
    private Integer totalUnits;

    @Column(name = "used_units", nullable = false)
    private Integer usedUnits;

    @Column(name = "available_units", nullable = false)
    private Integer availableUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "door_direction", length = 10)
    private DoorDirection doorDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_direction", length = 10)
    private ZoneDirection zoneDirection;

    @Column(name = "width")
    private Double width;

    @Column(name = "depth")
    private Double depth;

    @Column(name = "height")
    private Double height;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "max_power_capacity")
    private Double maxPowerCapacity;

    @Column(name = "current_power_usage")
    private Double currentPowerUsage;

    @Column(name = "measured_power")
    private Double measuredPower;

    @Column(name = "max_weight_capacity")
    private Double maxWeightCapacity;

    @Column(name = "current_weight")
    private Double currentWeight;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "management_number", length = 100)
    private String managementNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private RackStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "rack_type", length = 20)
    private RackType rackType;

    @Column(name = "color_code", length = 20)
    private String colorCode;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "manager_id", nullable = false, length = 50)
    private String managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DataCenter datacenter;

}
