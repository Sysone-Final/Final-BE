package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.company.domain.Company;

/**
 * 회사 목록 조회 응답 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 응답 데이터 전달
 * - 최소 정보만 포함: 목록 조회는 간략한 정보만 필요
 * - DetailResponse와의 차이: 필수 정보만 선별적으로 포함
 *
 * List용 DTO를 별도로 만드는 이유:
 * 1. 네트워크 트래픽 절감 (불필요한 정보 제외)
 * 2. 응답 속도 향상 (데이터 크기 감소)
 * 3. 화면 표시에 필요한 정보만 전달
 */
@Builder // 빌더 패턴 지원
public record CompanyListResponse(
        // === 식별자 ===
        Long id, // 회사 고유 식별자 (상세 조회로 이동할 때 필요)

        // === 기본 정보 (목록에서 보여줄 최소 정보) ===
        String code, // 회사 코드 (예: COMP001)
        // 코드는 화면에서 회사를 구분하는 데 사용

        String name, // 회사명 (예: 테크놀로지 주식회사)
        // 회사명은 목록에서 가장 중요한 정보

        // === 추가 정보 (목록에서 유용한 정보) ===
        String businessNumber, // 사업자등록번호 (목록에서 참고용)

        String phone, // 대표 전화번호 (빠른 연락을 위해 포함)

        String industry, // 업종 (목록에서 필터링/정렬 시 유용)

        Integer employeeCount // 직원 수 (회사 규모 파악용)

        // === 제외된 정보 (목록에서 불필요) ===
        // - address: 목록에서는 주소가 필요 없음 (상세 조회에서만)
        // - description: 긴 설명은 목록에서 불필요
        // - website, email, fax: 목록에서는 덜 중요
        // - logoUrl: 목록에서 로고를 보여주려면 포함 가능 (필요시 추가)
        // - createdAt, updatedAt: 목록에서는 시간 정보가 덜 중요
) {
    /**
     * Entity → DTO 변환 (정적 팩토리 메서드)
     * Company 엔티티를 받아서 CompanyListResponse DTO로 변환
     *
     * @param company Company 엔티티 객체
     * @return CompanyListResponse DTO 객체
     *
     * DetailResponse와의 차이:
     * - from() 메서드는 동일한 패턴이지만 포함되는 필드가 다름
     * - 목록용은 간략한 정보만, 상세용은 모든 정보 포함
     */
    public static CompanyListResponse from(Company company) {
        // === null 체크 ===
        if (company == null) {
            throw new IllegalArgumentException("Company 엔티티가 null입니다.");
        }

        // === Builder 패턴으로 DTO 생성 ===
        return CompanyListResponse.builder() // CompanyListResponse 빌더 시작
                // === 필수 정보만 설정 ===
                .id(company.getId()) // 회사 ID (상세 조회로 이동할 때 사용)
                .code(company.getCode()) // 회사 코드
                .name(company.getName()) // 회사명
                .businessNumber(company.getBusinessNumber()) // 사업자등록번호
                .phone(company.getPhone()) // 전화번호
                .industry(company.getIndustry()) // 업종
                .employeeCount(company.getEmployeeCount()) // 직원 수
                .build(); // CompanyListResponse DTO 객체 최종 생성

        // DetailResponse에 비해 훨씬 적은 필드만 포함
        // 네트워크 트래픽과 JSON 파싱 비용 절감
    }

}