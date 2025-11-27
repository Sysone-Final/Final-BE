/**
 * 작성자: 황요한
 * 데이터센터 수정 요청 DTO
 * - 부분 수정 지원 (null 값은 무시됨)
 */
package org.example.finalbe.domains.datacenter.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record DataCenterUpdateRequest(

        @Size(max = 200, message = "데이터센터명은 200자 이하여야 합니다.")
        String name,            // 데이터센터명

        @Size(max = 500, message = "주소는 500자 이하여야 합니다.")
        String address,         // 주소

        String description      // 설명
) {
}
