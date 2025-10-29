package org.example.finalbe.domains.datacenter.dto;

import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 전산실 상세 조회 응답 DTO
 *
 * - Record: 불변 객체로 DTO를 정의 (Java 14+)
 * - Builder 패턴: 가독성 높은 객체 생성 지원
 * - from(): Entity → DTO 정적 팩토리 메서드
 * - 담당자 정보 포함: managerId, managerName, managerEmail
 * - 랙 정보 포함: maxRackCount, currentRackCount, availableRackCount
 */
@Builder // 빌더 패턴을 사용하여 가독성 높은 객체 생성
public record DataCenterDetailResponse(
        Long id, // 전산실 ID

        String name, // 전산실 이름

        String code, // 전산실 코드 (UNIQUE)

        String location, // 전산실 위치/주소

        String floor, // 전산실 층수

        Integer rows, // 랙 배치 행 수

        Integer columns, // 랙 배치 열 수

        String backgroundImageUrl, // 배경 이미지 URL (평면도)

        DataCenterStatus status, // 전산실 상태 (ACTIVE, INACTIVE, MAINTENANCE)

        String description, // 전산실 설명

        BigDecimal totalArea, // 총 면적 (m²)

        BigDecimal totalPowerCapacity, // 총 전력 용량 (kW)

        BigDecimal totalCoolingCapacity, // 총 냉각 용량 (kW)

        Integer maxRackCount, // 최대 랙 개수

        Integer currentRackCount, // 현재 랙 개수

        Integer availableRackCount, // 사용 가능한 랙 개수 (maxRackCount - currentRackCount)

        BigDecimal temperatureMin, // 최저 허용 온도 (℃)

        BigDecimal temperatureMax, // 최고 허용 온도 (℃)

        BigDecimal humidityMin, // 최저 허용 습도 (%)

        BigDecimal humidityMax, // 최고 허용 습도 (%)

        Long managerId, // 담당자 ID

        String managerName, // 담당자 이름

        String managerEmail, // 담당자 이메일

        LocalDateTime createdAt, // 생성 시간

        LocalDateTime updatedAt // 수정 시간
) {
    /**
     * Entity를 DTO로 변환
     * Service 계층에서 호출하여 응답 데이터 생성
     *
     * @param dataCenter 전산실 엔티티
     * @return DataCenterDetailResponse DTO
     */
    public static DataCenterDetailResponse from(DataCenter dataCenter) {
        return DataCenterDetailResponse.builder()
                .id(dataCenter.getId()) // 전산실 ID
                .name(dataCenter.getName()) // 전산실 이름
                .code(dataCenter.getCode()) // 전산실 코드
                .location(dataCenter.getLocation()) // 위치/주소
                .floor(dataCenter.getFloor()) // 층수
                .rows(dataCenter.getRows()) // 행 수
                .columns(dataCenter.getColumns()) // 열 수
                .backgroundImageUrl(dataCenter.getBackgroundImageUrl()) // 배경 이미지 URL
                .status(dataCenter.getStatus()) // 상태
                .description(dataCenter.getDescription()) // 설명
                .totalArea(dataCenter.getTotalArea()) // 총 면적
                .totalPowerCapacity(dataCenter.getTotalPowerCapacity()) // 총 전력 용량
                .totalCoolingCapacity(dataCenter.getTotalCoolingCapacity()) // 총 냉각 용량
                .maxRackCount(dataCenter.getMaxRackCount()) // 최대 랙 개수
                .currentRackCount(dataCenter.getCurrentRackCount()) // 현재 랙 개수
                .availableRackCount(dataCenter.getAvailableRackCount()) // 사용 가능한 랙 개수 (계산된 값)
                .temperatureMin(dataCenter.getTemperatureMin()) // 최저 온도
                .temperatureMax(dataCenter.getTemperatureMax()) // 최고 온도
                .humidityMin(dataCenter.getHumidityMin()) // 최저 습도
                .humidityMax(dataCenter.getHumidityMax()) // 최고 습도
                .managerId(dataCenter.getManager().getId()) // 담당자 ID (ManyToOne 관계)
                .managerName(dataCenter.getManager().getName()) // 담당자 이름
                .managerEmail(dataCenter.getManager().getEmail()) // 담당자 이메일
                .createdAt(dataCenter.getCreatedAt()) // 생성 시간 (BaseTimeEntity)
                .updatedAt(dataCenter.getUpdatedAt()) // 수정 시간 (BaseTimeEntity)
                .build(); // Builder 패턴으로 객체 생성
    }
}