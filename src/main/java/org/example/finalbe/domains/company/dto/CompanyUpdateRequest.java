package org.example.finalbe.domains.company.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * 회사 수정 요청 DTO
 */
@Builder
public record CompanyUpdateRequest(
        @Size(max = 200, message = "회사명은 200자를 초과할 수 없습니다.")
        String name,

        @Size(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
        String businessNumber,

        @Size(max = 100, message = "대표자명은 100자를 초과할 수 없습니다.")
        String ceoName,

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 02-1234-5678)")
        String phone,

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "팩스 형식이 올바르지 않습니다. (예: 02-1234-5678)")
        String fax,

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
        String email,

        @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다.")
        String address,

        @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
                message = "올바른 웹사이트 URL 형식이 아닙니다.")
        String website,

        @Size(max = 100, message = "업종은 100자를 초과할 수 없습니다.")
        String industry,

        @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다.")
        String description,

        @Min(value = 1, message = "직원 수는 1명 이상이어야 합니다.")
        @Max(value = 1000000, message = "직원 수는 1,000,000명을 초과할 수 없습니다.")
        Integer employeeCount,

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",
                message = "설립일 형식이 올바르지 않습니다. (예: 2020-01-01)")
        String establishedDate,

        String logoUrl
) {
}