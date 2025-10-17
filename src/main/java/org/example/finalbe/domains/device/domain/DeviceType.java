package org.example.finalbe.domains.device.domain;


import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.enumdir.DeviceCategory;

@Entity
@Table(name = "device_type")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_type_id")
    private Long id;

    @Column(name = "type_name", nullable = false, length = 50)
    private String typeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private DeviceCategory category; // 냉각/네트워크/보안/기타

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "description", length = 255)
    private String description;

    @Lob
    @Column(name = "attributes_template")
    private String attributesTemplate; // JSON (ex: {"power":"500W","voltage":"220V"})
}
