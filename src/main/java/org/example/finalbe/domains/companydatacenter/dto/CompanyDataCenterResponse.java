package org.example.finalbe.domains.companydatacenter.dto;

import lombok.Builder;
import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;

import java.time.LocalDateTime;

/**
 * 회사-전산실 매핑 응답 DTO
 * 매핑 정보와 회사/전산실 이름을 함께 반환
 */
@Builder // 빌더 패턴
public record CompanyDataCenterResponse(
        Long id, // 매핑 고유 식별자
        Long companyId, // 회사 ID
        String companyName, // 회사명 (예: A회사)
        Long dataCenterId, // 전산실 ID
        String dataCenterName, // 전산실명 (예: 서울 데이터센터)
        String description, // 매핑 설명 (예: 2025년 계약)
        String grantedBy, // 권한 부여자 (예: admin)
        LocalDateTime createdAt // 생성 시간
) {
    /**
     * Entity → DTO 변환
     */
    public static CompanyDataCenterResponse from(CompanyDataCenter companyDataCenter) {
        return CompanyDataCenterResponse.builder()
                .id(companyDataCenter.getId()) // 매핑 ID
                .companyId(companyDataCenter.getCompany().getId()) // 회사 ID
                .companyName(companyDataCenter.getCompany().getName()) // 회사명
                .dataCenterId(companyDataCenter.getDataCenter().getId()) // 전산실 ID
                .dataCenterName(companyDataCenter.getDataCenter().getName()) // 전산실명
                .description(companyDataCenter.getDescription()) // 설명
                .grantedBy(companyDataCenter.getGrantedBy()) // 권한 부여자
                .createdAt(companyDataCenter.getCreatedAt()) // 생성 시간
                .build(); // DTO 객체 생성
    }
}