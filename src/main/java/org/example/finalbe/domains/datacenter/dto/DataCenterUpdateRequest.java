package org.example.finalbe.domains.datacenter.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 데이터센터 수정 요청 DTO
 */
@Builder
public record DataCenterUpdateRequest(
        @Size(max = 200, message = "데이터센터명은 200자 이하여야 합니다.")
        String name,

        @Size(max = 500, message = "주소는 500자 이하여야 합니다.")
        String address,

        String description

) {
}