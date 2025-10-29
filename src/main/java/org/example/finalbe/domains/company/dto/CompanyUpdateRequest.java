package org.example.finalbe.domains.company.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * 회사 수정 요청 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 요청 데이터 전달
 * - Bean Validation: 입력값 검증
 * - 부분 수정 지원: 모든 필드가 선택사항 (null 허용)
 *
 * CreateRequest와의 차이점:
 * - code는 변경 불가 (수정 시 제외)
 * - 모든 필드가 선택사항 (부분 수정 가능)
 * - @NotBlank 대신 @Size만 사용
 */
@Builder // 빌더 패턴 지원
public record CompanyUpdateRequest(
        // === 기본 정보 (모두 선택사항) ===

        @Size(max = 200, message = "회사명은 200자를 초과할 수 없습니다.")
        // @NotBlank가 없으므로 null 허용 (선택적 수정)
        // null이면 기존 값 유지, 값이 있으면 해당 값으로 변경
        String name, // 회사명 (예: 테크놀로지 주식회사)

        // === 사업자 정보 (모두 선택사항) ===

        @Size(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
        String businessNumber, // 사업자등록번호 (예: 123-45-67890)

        @Size(max = 100, message = "대표자명은 100자를 초과할 수 없습니다.")
        String ceoName, // 대표자명 (예: 홍길동)

        // === 연락처 정보 (모두 선택사항) ===

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 02-1234-5678)")
        // 정규표현식으로 전화번호 형식 검증
        // null이면 검증 통과 (선택사항)
        String phone, // 대표 전화번호

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "팩스 형식이 올바르지 않습니다. (예: 02-1234-5678)")
        String fax, // 팩스번호

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        // 이메일 형식 검증 (null이면 검증 통과)
        @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
        String email, // 대표 이메일

        // === 주소 및 웹사이트 (모두 선택사항) ===

        @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다.")
        String address, // 본사 주소

        @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
                message = "올바른 웹사이트 URL 형식이 아닙니다.")
        // URL 형식 검증 (null이면 검증 통과)
        String website, // 회사 웹사이트

        // === 회사 상세 정보 (모두 선택사항) ===

        @Size(max = 100, message = "업종은 100자를 초과할 수 없습니다.")
        String industry, // 업종

        @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다.")
        String description, // 회사 설명

        @Min(value = 1, message = "직원 수는 1명 이상이어야 합니다.")
        // 최소값 검증 (null이면 검증 통과)
        @Max(value = 1000000, message = "직원 수는 1,000,000명을 초과할 수 없습니다.")
        // 최대값 검증
        Integer employeeCount, // 직원 수

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",
                message = "설립일 형식이 올바르지 않습니다. (예: 2020-01-01)")
        // YYYY-MM-DD 형식 검증
        String establishedDate, // 설립일

        String logoUrl // 회사 로고 이미지 URL
) {
    /**
     * 부분 수정 예시:
     *
     * 1. 회사명만 수정:
     *    CompanyUpdateRequest.builder()
     *        .name("새로운 회사명")
     *        .build()
     *    → name만 업데이트, 나머지 필드는 기존 값 유지
     *
     * 2. 연락처만 수정:
     *    CompanyUpdateRequest.builder()
     *        .phone("02-9999-9999")
     *        .email("new@email.com")
     *        .build()
     *    → phone, email만 업데이트
     *
     * 3. 전체 수정:
     *    CompanyUpdateRequest.builder()
     *        .name("새 회사명")
     *        .phone("02-1111-1111")
     *        .address("새 주소")
     *        ... (모든 필드)
     *        .build()
     *
     * Service 계층에서 Company.updateInfo() 메서드가
     * null이 아닌 필드만 선택적으로 업데이트함
     */

    // UpdateRequest는 toEntity() 메서드가 없음
    // 이유: 기존 Entity를 조회한 후 updateInfo() 메서드로 수정하기 때문
    //
    // 흐름:
    // 1. Repository에서 기존 Company 조회
    // 2. Company.updateInfo(request의 필드들)로 수정
    // 3. JPA의 Dirty Checking으로 자동 UPDATE
    //
    // 새로운 Entity를 생성하지 않고 기존 Entity를 수정하는 것이 핵심!
}