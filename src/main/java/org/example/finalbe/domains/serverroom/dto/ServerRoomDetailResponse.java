/**
 * 작성자: 황요한
 * 서버실 상세 조회 응답 DTO
 */
package org.example.finalbe.domains.serverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ServerRoomDetailResponse(
        Long id,                          // ID
        String name,                      // 이름
        String code,                      // 코드
        String location,                  // 위치
        Integer floor,                    // 층수
        Integer rows,                     // 행 개수
        Integer columns,                  // 열 개수
        ServerRoomStatus status,          // 상태
        String description,               // 설명
        BigDecimal totalArea,             // 총 면적
        BigDecimal totalPowerCapacity,    // 전력 용량
        BigDecimal totalCoolingCapacity,  // 냉각 용량
        BigDecimal temperatureMin,        // 온도 최소
        BigDecimal temperatureMax,        // 온도 최대
        BigDecimal humidityMin,           // 습도 최소
        BigDecimal humidityMax,           // 습도 최대
        Long dataCenterId,                // 데이터센터 ID
        String dataCenterName,            // 데이터센터 이름
        LocalDateTime createdAt,          // 생성일
        LocalDateTime updatedAt           // 수정일
) {
    /** Entity → DTO 변환 */
    public static ServerRoomDetailResponse from(ServerRoom s) {
        return ServerRoomDetailResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .code(s.getCode())
                .location(s.getLocation())
                .floor(s.getFloor())
                .rows(s.getRows())
                .columns(s.getColumns())
                .status(s.getStatus())
                .description(s.getDescription())
                .totalArea(s.getTotalArea())
                .totalPowerCapacity(s.getTotalPowerCapacity())
                .totalCoolingCapacity(s.getTotalCoolingCapacity())
                .temperatureMin(s.getTemperatureMin())
                .temperatureMax(s.getTemperatureMax())
                .humidityMin(s.getHumidityMin())
                .humidityMax(s.getHumidityMax())
                .dataCenterId(s.getDataCenter() != null ? s.getDataCenter().getId() : null)
                .dataCenterName(s.getDataCenter() != null ? s.getDataCenter().getName() : null)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
