package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.math.BigDecimal;

/**
 * 전산실 목록 조회 응답 DTO
 *
 * - Record: 불변 객체로 DTO를 정의 (Java 14+)
 * - Builder 패턴: 가독성 높은 객체 생성 지원
 * - from(): Entity → DTO 정적 팩토리 메서드
 * - 목록 조회용: 상세 정보는 제외하고 필수 정보만 포함
 * - 성능 최적화: 불필요한 필드 제외하여 네트워크 트래픽 감소
 */
@Builder // 빌더 패턴을 사용하여 가독성 높은 객체 생성
public record DataCenterListResponse(
        Long id, // 전산실 ID

        String name, // 전산실 이름

        String code, // 전산실 코드 (UNIQUE)

        String location, // 전산실 위치/주소 (간략 표시)

        String floor, // 전산실 층수

        DataCenterStatus status, // 전산실 상태 (ACTIVE, INACTIVE, MAINTENANCE)

        Integer maxRackCount, // 최대 랙 개수

        Integer currentRackCount, // 현재 랙 개수

        Integer availableRackCount, // 사용 가능한 랙 개수 (계산된 값)

        BigDecimal totalArea, // 총 면적 (m²)

        String managerName // 담당자 이름 (간략 표시)
) {
    /**
     * Entity를 DTO로 변환
     * Service 계층에서 호출하여 목록 데이터 생성
     *
     * @param dataCenter 전산실 엔티티
     * @return DataCenterListResponse DTO
     */
    public static DataCenterListResponse from(DataCenter dataCenter) {
        return DataCenterListResponse.builder()
                .id(dataCenter.getId()) // 전산실 ID
                .name(dataCenter.getName()) // 전산실 이름
                .code(dataCenter.getCode()) // 전산실 코드
                .location(dataCenter.getLocation()) // 위치/주소
                .floor(dataCenter.getFloor()) // 층수
                .status(dataCenter.getStatus()) // 상태
                .maxRackCount(dataCenter.getMaxRackCount()) // 최대 랙 개수
                .currentRackCount(dataCenter.getCurrentRackCount()) // 현재 랙 개수
                .availableRackCount(dataCenter.getAvailableRackCount()) // 사용 가능한 랙 개수 (계산된 값)
                .totalArea(dataCenter.getTotalArea()) // 총 면적
                .managerName(dataCenter.getManager().getName()) // 담당자 이름 (ManyToOne 관계)
                .build(); // Builder 패턴으로 객체 생성
    }
}