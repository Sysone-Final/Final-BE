package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.time.LocalDateTime;

/**
 * 회사의 전산실 목록 조회 응답 DTO (Data Transfer Object)
 *
 * - Record: 불변 객체로 응답 데이터 전달
 * - 용도: 특정 회사가 접근 가능한 전산실(데이터센터) 목록 조회
 * - CompanyDataCenter 매핑 정보와 DataCenter 정보 결합
 *
 * 이 DTO가 필요한 이유:
 * - 회사와 전산실은 다대다 관계 (한 회사가 여러 전산실 접근 가능)
 * - CompanyDataCenter는 매핑 테이블 (중간 테이블)
 * - 화면에서는 회사가 어떤 전산실들에 접근할 수 있는지 보여줘야 함
 */
@Builder // 빌더 패턴 지원
public record CompanyDataCenterListResponse(
        // === 전산실 기본 정보 ===
        Long dataCenterId, // 전산실 고유 식별자
        // 전산실 ID는 상세 조회로 이동할 때 필요

        String dataCenterCode, // 전산실 코드 (예: DC001, DC002)
        // 전산실을 구분하는 코드

        String dataCenterName, // 전산실명 (예: 서울 IDC 1센터)
        // 전산실의 이름

        // === 전산실 위치 정보 ===
        String location, // 전산실 위치 (예: 서울시 금천구)
        // 목록에서 전산실의 위치를 한눈에 확인

        // === 전산실 담당자 정보 ===
        String managerName, // 전산실 담당자 이름
        // 문의 시 연락할 담당자

        String managerPhone, // 전산실 담당자 전화번호
        // 빠른 연락을 위한 정보

        // === 매핑 정보 (언제 이 회사에게 접근 권한이 부여되었는지) ===
        LocalDateTime grantedAt // 접근 권한 부여 시간
        // CompanyDataCenter의 createdAt을 매핑
        // 이 회사가 언제부터 이 전산실에 접근할 수 있게 되었는지

        // === 제외된 정보 (목록에서 불필요) ===
        // - DataCenter의 상세 정보 (description, 랙 수 등)
        // - CompanyDataCenter의 description (매핑 설명)
        // 이런 정보는 전산실 상세 조회에서만 제공
) {
    /**
     * DataCenter와 매핑 시간으로 DTO 생성 (정적 팩토리 메서드)
     *
     * @param dataCenter DataCenter 엔티티 객체
     * @param grantedAt 접근 권한 부여 시간 (CompanyDataCenter의 createdAt)
     * @return CompanyDataCenterListResponse DTO 객체
     *
     * 이 메서드를 사용하는 흐름:
     * 1. CompanyDataCenter 매핑 테이블 조회
     * 2. 각 매핑에서 DataCenter와 createdAt 추출
     * 3. from() 메서드로 DTO 생성
     */
    public static CompanyDataCenterListResponse from(DataCenter dataCenter, LocalDateTime grantedAt) {
        // === null 체크 ===
        if (dataCenter == null) {
            throw new IllegalArgumentException("DataCenter 엔티티가 null입니다.");
        }
        if (grantedAt == null) {
            throw new IllegalArgumentException("grantedAt이 null입니다.");
        }

        // === Builder 패턴으로 DTO 생성 ===
        return CompanyDataCenterListResponse.builder() // CompanyDataCenterListResponse 빌더 시작
                // === 전산실 기본 정보 설정 ===
                .dataCenterId(dataCenter.getId()) // 전산실 ID
                // DataCenter의 ID를 가져옴

                .dataCenterCode(dataCenter.getCode()) // 전산실 코드
                // DataCenter의 code 필드

                .dataCenterName(dataCenter.getName()) // 전산실명
                // DataCenter의 name 필드

                // === 전산실 위치 정보 설정 ===
                .location(dataCenter.getLocation()) // 전산실 위치
                // DataCenter의 location 필드

                // === 전산실 담당자 정보 설정 ===
                .managerName(dataCenter.getManager() != null ? dataCenter.getManager().getName() : null)
                // DataCenter의 manager(Member)가 있으면 이름 추출, 없으면 null
                // 삼항 연산자로 null-safe 처리

                .managerPhone(dataCenter.getManager() != null ? dataCenter.getManager().getPhone() : null)
                // DataCenter의 manager가 있으면 전화번호 추출, 없으면 null

                // === 매핑 정보 설정 ===
                .grantedAt(grantedAt) // 접근 권한 부여 시간
                // CompanyDataCenter의 createdAt을 그대로 전달받음

                .build(); // CompanyDataCenterListResponse DTO 객체 최종 생성
    }

}