package org.example.finalbe.domains.device.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.enumdir.DeviceCategory;

/**
 * 장치 타입 엔티티
 * server, door, climatic_chamber, fire_extinguisher, thermometer, aircon
 */
@Entity
@Table(name = "device_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_type_id")
    private Long id;

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName;  // server, door, climatic_chamber 등

    @Column(name = "category", length = 50)
    @Enumerated(EnumType.STRING)
    private DeviceCategory category;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "attributes_template", columnDefinition = "TEXT")
    private String attributesTemplate;  // JSON 형태로 저장 가능
}