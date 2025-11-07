package org.example.finalbe.domains.companyserverroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * 회사-전산실 매핑 생성 요청 DTO
 */
@Builder
public record CompanyServerRoomCreateRequest(

        @NotNull(message = "회사를 선택해주세요.")
        @Min(value = 1, message = "유효하지 않은 회사 ID입니다.")
        Long companyId,

        @NotEmpty(message = "전산실을 하나 이상 선택해주세요.")
        List<@NotNull(message = "전산실 ID는 null일 수 없습니다.")
        @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.")
                Long> serverRoomIds,

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
        String description
) {
}