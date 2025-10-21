package org.example.finalbe.domains.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DeviceCategory;
import org.example.finalbe.domains.device.domain.DeviceType;


@Builder
public record DeviceTypeCreateRequest(
        @NotBlank(message = "타입명을 입력해주세요.")
        @Size(max = 50, message = "타입명은 50자를 초과할 수 없습니다.")
        String typeName,

        String category,

        @Size(max = 500, message = "아이콘 URL은 500자를 초과할 수 없습니다.")
        String iconUrl,

        @Size(max = 255, message = "설명은 255자를 초과할 수 없습니다.")
        String description,

        String attributesTemplate
) {
    public DeviceType toEntity() {
        return DeviceType.builder()
                .typeName(this.typeName)
                .category(this.category != null ? DeviceCategory.valueOf(this.category) : null)
                .iconUrl(this.iconUrl)
                .description(this.description)
                .attributesTemplate(this.attributesTemplate)
                .build();
    }
}