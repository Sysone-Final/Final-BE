package org.example.finalbe.domains.companydatacenter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * 회사-전산실 매핑 생성 요청 DTO
 * 한 회사에 여러 전산실을 한 번에 매핑
 * Bean Validation으로 입력값 자동 검증
 */
@Builder // 빌더 패턴
public record CompanyDataCenterCreateRequest(

        @NotNull(message = "회사를 선택해주세요.") // null 불허
        @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // 1 이상
        Long companyId, // 매핑할 회사 ID

        @NotEmpty(message = "전산실을 하나 이상 선택해주세요.") // 빈 리스트 불허
        List<@NotNull(message = "전산실 ID는 null일 수 없습니다.") // 각 요소 null 체크
        @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") // 각 요소 1 이상
                Long> dataCenterIds, // 매핑할 전산실 ID 목록 (예: [1, 2, 3])

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.") // 최대 길이 제한
        String description // 매핑 설명 (선택, 예: "2025년 계약")
) {
}