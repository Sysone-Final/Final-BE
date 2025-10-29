package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;

import java.time.LocalDateTime;

/**
 * 회사 상세 조회 응답 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 응답 데이터 전달
 * - Entity → DTO 변환: from() 정적 팩토리 메서드 제공
 * - 모든 회사 정보 포함 (상세 조회용)
 *
 * Response DTO를 사용하는 이유:
 * 1. Entity 직접 노출 방지 (보안)
 * 2. 필요한 정보만 선택적으로 반환
 * 3. JSON 직렬화 최적화
 * 4. API 응답 형식 일관성 유지
 */
@Builder // 빌더 패턴 지원
public record CompanyDetailResponse(
        // === 식별자 ===
        Long id, // 회사 고유 식별자

        // === 기본 정보 ===
        String code, // 회사 코드 (예: COMP001)
        String name, // 회사명 (예: 테크놀로지 주식회사)

        // === 사업자 정보 ===
        String businessNumber, // 사업자등록번호 (예: 123-45-67890)
        String ceoName, // 대표자명 (예: 홍길동)

        // === 연락처 정보 ===
        String phone, // 대표 전화번호 (예: 02-1234-5678)
        String fax, // 팩스번호 (예: 02-1234-5679)
        String email, // 대표 이메일 (예: contact@company.com)

        // === 주소 및 웹사이트 ===
        String address, // 본사 주소 (예: 서울시 강남구 테헤란로 123)
        String website, // 회사 웹사이트 (예: https://www.company.com)

        // === 회사 상세 정보 ===
        String industry, // 업종 (예: IT, 제조업, 금융)
        String description, // 회사 설명 (장문의 소개글)
        Integer employeeCount, // 직원 수 (예: 100)
        String establishedDate, // 설립일 (예: 2020-01-01)
        String logoUrl, // 회사 로고 이미지 URL

        // === 메타 정보 (생성/수정 시간) ===
        LocalDateTime createdAt, // 생성 시간 (BaseTimeEntity에서 자동 관리)
        LocalDateTime updatedAt // 수정 시간 (BaseTimeEntity에서 자동 관리)

        // delYn은 포함하지 않음 (내부 관리용이므로 외부에 노출할 필요 없음)
) {
    /**
     * Entity → DTO 변환 (정적 팩토리 메서드)
     * Company 엔티티를 받아서 CompanyDetailResponse DTO로 변환
     *
     * @param company Company 엔티티 객체
     * @return CompanyDetailResponse DTO 객체
     *
     * 정적 팩토리 메서드를 사용하는 이유:
     * 1. 생성자보다 의미 전달이 명확 (from이라는 이름으로 변환 의도 표현)
     * 2. DTO 생성 로직을 DTO 내부에 캡슐화
     * 3. Service에서 변환 로직 중복 제거
     */
    public static CompanyDetailResponse from(Company company) {
        // === null 체크 ===
        // company가 null이면 예외 발생 (방어적 프로그래밍)
        if (company == null) {
            throw new IllegalArgumentException("Company 엔티티가 null입니다.");
        }

        // === Builder 패턴으로 DTO 생성 ===
        return CompanyDetailResponse.builder() // CompanyDetailResponse 빌더 시작
                // === 식별자 설정 ===
                .id(company.getId()) // Entity의 id를 DTO에 설정
                // company.getId(): Entity의 getter 호출

                // === 기본 정보 설정 ===
                .code(company.getCode()) // 회사 코드 설정
                .name(company.getName()) // 회사명 설정

                // === 사업자 정보 설정 ===
                .businessNumber(company.getBusinessNumber()) // 사업자등록번호 설정
                .ceoName(company.getCeoName()) // 대표자명 설정

                // === 연락처 정보 설정 ===
                .phone(company.getPhone()) // 전화번호 설정
                .fax(company.getFax()) // 팩스번호 설정
                .email(company.getEmail()) // 이메일 설정

                // === 주소 및 웹사이트 설정 ===
                .address(company.getAddress()) // 주소 설정
                .website(company.getWebsite()) // 웹사이트 설정

                // === 상세 정보 설정 ===
                .industry(company.getIndustry()) // 업종 설정
                .description(company.getDescription()) // 설명 설정
                .employeeCount(company.getEmployeeCount()) // 직원 수 설정
                .establishedDate(company.getEstablishedDate()) // 설립일 설정
                .logoUrl(company.getLogoUrl()) // 로고 URL 설정

                // === 메타 정보 설정 ===
                .createdAt(company.getCreatedAt()) // 생성 시간 설정
                // BaseTimeEntity에서 상속받은 필드
                .updatedAt(company.getUpdatedAt()) // 수정 시간 설정

                .build(); // CompanyDetailResponse DTO 객체 최종 생성

        // 모든 필드를 그대로 복사 (Entity의 모든 정보를 DTO에 포함)
        // 상세 조회이므로 가능한 많은 정보를 제공
    }

}