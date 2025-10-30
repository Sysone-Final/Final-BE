package org.example.finalbe.domains.device.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.enumdir.DeviceCategory;

/**
 * 장치 타입 엔티티
 */
@Entity
@Table(name = "device_type")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DeviceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_type_id")
    private Long id; // 장치 타입 ID

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName; // 타입명 (server, door, climatic_chamber 등)

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private DeviceCategory category; // 카테고리

    @Column(name = "icon_url", length = 500)
    private String iconUrl; // 아이콘 URL

    @Column(name = "description", length = 255)
    private String description; // 설명

    @Column(name = "attributes_template", columnDefinition = "TEXT")
    private String attributesTemplate; // 속성 템플릿 (JSON 형태)
}