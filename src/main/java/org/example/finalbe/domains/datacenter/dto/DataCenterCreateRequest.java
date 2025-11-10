// src/main/java/org/example/finalbe/domains/datacenter/dto/DataCenterCreateRequest.java

package org.example.finalbe.domains.datacenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

/**
 * 데이터센터 생성 요청 DTO
 */
@Builder
public record DataCenterCreateRequest(
        @NotBlank(message = "데이터센터 코드는 필수입니다.")
        @Size(max = 50, message = "데이터센터 코드는 50자 이하여야 합니다.")
        String code,

        @NotBlank(message = "데이터센터명은 필수입니다.")
        @Size(max = 200, message = "데이터센터명은 200자 이하여야 합니다.")
        String name,


        @Size(max = 500, message = "주소는 500자 이하여야 합니다.")
        String address,

        String description

) {
    /**
     * DTO → Entity 변환
     */
    public DataCenter toEntity() {
        return DataCenter.builder()
                .code(code)
                .name(name)
                .address(address)
                .description(description)
                .build();
    }
}