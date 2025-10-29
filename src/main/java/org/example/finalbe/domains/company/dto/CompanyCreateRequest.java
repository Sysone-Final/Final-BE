package org.example.finalbe.domains.company.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;

/**
 * 회사 생성 요청 DTO (Data Transfer Object)
 *
 * - Record: Java 16+ 불변 객체, 자동으로 생성자, getter, equals, hashCode, toString 생성
 * - Bean Validation: @NotBlank, @Size 등으로 입력값 검증
 * - DTO 패턴: Controller와 Entity 사이의 데이터 전송 객체
 */
@Builder // 빌더 패턴 지원 (선택적 필드 설정 시 가독성 향상)
public record CompanyCreateRequest(
        // === 필수 필드 (회사 코드, 회사명) ===

        @NotBlank(message = "회사 코드를 입력해주세요.")
        // @NotBlank: null, 빈 문자열(""), 공백만 있는 문자열("   ") 모두 불허
        // String 타입에만 사용 가능 (숫자 타입은 @NotNull 사용)
        @Size(max = 50, message = "회사 코드는 50자를 초과할 수 없습니다.")
        // @Size: 문자열 길이 제한 (DB 컬럼 길이와 일치시켜야 함)
        String code, // 회사 코드 (예: COMP001, COMP002)

        @NotBlank(message = "회사명을 입력해주세요.")
        // 회사명은 필수 입력 (null 또는 빈 문자열 불허)
        @Size(max = 200, message = "회사명은 200자를 초과할 수 없습니다.")
        // DB의 VARCHAR(200)과 일치
        String name, // 회사명 (예: 테크놀로지 주식회사)

        // === 선택 필드 (사업자정보) ===

        @Size(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
        // @NotBlank가 없으므로 선택사항 (null 허용)
        String businessNumber, // 사업자등록번호 (예: 123-45-67890)

        @Size(max = 100, message = "대표자명은 100자를 초과할 수 없습니다.")
        String ceoName, // 대표자명 (예: 홍길동)

        // === 선택 필드 (연락처) ===

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 02-1234-5678)")
        // @Pattern: 정규표현식으로 형식 검증
        // ^\\d{2,3}: 2~3자리 숫자로 시작 (지역번호)
        // -\\d{3,4}: 하이픈 + 3~4자리 숫자 (국번)
        // -\\d{4}$: 하이픈 + 4자리 숫자로 끝 (번호)
        // 예: 02-1234-5678, 031-123-4567
        String phone, // 대표 전화번호

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$",
                message = "팩스 형식이 올바르지 않습니다. (예: 02-1234-5678)")
        // 팩스도 전화번호와 동일한 형식
        String fax, // 팩스번호

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        // @Email: 이메일 형식 검증 (예: user@domain.com)
        // RFC 5322 표준을 따르는 이메일 형식 체크
        @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
        String email, // 대표 이메일

        // === 선택 필드 (주소 및 웹사이트) ===

        @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다.")
        String address, // 본사 주소 (예: 서울시 강남구 테헤란로 123)

        @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
                message = "올바른 웹사이트 URL 형식이 아닙니다.")
        // URL 형식 검증 정규표현식
        // ^(https?://)?:  http:// 또는 https://로 시작 (선택사항)
        // ([\\da-z.-]+): 도메인명 (숫자, 소문자, 점, 하이픈 허용)
        // \\.([a-z.]{2,6}): 최상위 도메인 (.com, .co.kr 등)
        // ([/\\w .-]*)*: 경로 (슬래시, 문자, 숫자, 공백, 점, 하이픈 허용)
        // 예: https://www.example.com, http://example.co.kr/path
        String website, // 회사 웹사이트

        // === 선택 필드 (회사 상세 정보) ===

        @Size(max = 100, message = "업종은 100자를 초과할 수 없습니다.")
        String industry, // 업종 (예: IT, 제조업, 금융)

        @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다.")
        String description, // 회사 설명 (장문의 소개글)

        @Min(value = 1, message = "직원 수는 1명 이상이어야 합니다.")
        // @Min: 최소값 제한 (0명 이하는 유효하지 않음)
        @Max(value = 1000000, message = "직원 수는 1,000,000명을 초과할 수 없습니다.")
        // @Max: 최대값 제한 (비현실적으로 큰 값 방지)
        Integer employeeCount, // 직원 수 (null 허용 → 선택사항)

        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",
                message = "설립일 형식이 올바르지 않습니다. (예: 2020-01-01)")
        // YYYY-MM-DD 형식 검증
        // ^\\d{4}: 4자리 연도
        // -\\d{2}: 하이픈 + 2자리 월
        // -\\d{2}$: 하이픈 + 2자리 일로 끝
        String establishedDate, // 설립일 (예: 2020-01-01)

        String logoUrl // 회사 로고 이미지 URL (검증 없음 → 자유 형식)
) {
    /**
     * DTO를 Entity로 변환하는 메서드
     * Request DTO의 일관된 패턴
     *
     * @return Company 엔티티 객체
     *
     * DTO → Entity 변환이 필요한 이유:
     * - Controller는 DTO만 다루고, Service는 Entity만 다룸 (계층 간 책임 분리)
     * - Entity 생성 로직을 DTO에 캡슐화하여 재사용성 향상
     * - Entity 직접 노출 방지 (보안 및 캡슐화)
     */
    public Company toEntity() {
        // === Builder 패턴으로 Company 엔티티 생성 ===
        return Company.builder() // Company 빌더 시작
                // === 필수 필드 설정 ===
                .code(this.code) // DTO의 code를 Entity에 설정
                // this.code: record의 필드 접근 (자동 생성된 getter)

                .name(this.name) // 회사명 설정

                // === 선택 필드 설정 (null 가능) ===
                .businessNumber(this.businessNumber) // 사업자등록번호 설정
                .ceoName(this.ceoName) // 대표자명 설정

                // === 연락처 설정 ===
                .phone(this.phone) // 전화번호 설정
                .fax(this.fax) // 팩스번호 설정
                .email(this.email) // 이메일 설정

                // === 주소 및 웹사이트 설정 ===
                .address(this.address) // 주소 설정
                .website(this.website) // 웹사이트 설정

                // === 상세 정보 설정 ===
                .industry(this.industry) // 업종 설정
                .description(this.description) // 설명 설정
                .employeeCount(this.employeeCount) // 직원 수 설정
                .establishedDate(this.establishedDate) // 설립일 설정
                .logoUrl(this.logoUrl) // 로고 URL 설정

                .build(); // Company 엔티티 객체 최종 생성
        // 빌더 패턴의 build()는 설정된 값으로 최종 객체를 생성
        // delYn은 Entity의 @Builder.Default로 자동 설정됨 (DelYN.N)
    }
}