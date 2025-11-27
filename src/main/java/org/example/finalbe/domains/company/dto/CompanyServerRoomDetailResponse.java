/**
 * 작성자: 황요한
 * 회사가 접근 권한을 가진 서버실의 상세 정보를 제공하는 DTO
 */
package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CompanyServerRoomDetailResponse(
        Long id,
        String name,
        String code,
        String location,
        Integer floor,
        Integer rows,
        Integer columns,
        ServerRoomStatus status,
        String description,

        // 서버실 인프라 용량
        BigDecimal totalArea,
        BigDecimal totalPowerCapacity,
        BigDecimal totalCoolingCapacity,

        // 현재 현황
        Integer currentRackCount,
        BigDecimal temperatureMin,
        BigDecimal temperatureMax,
        BigDecimal humidityMin,
        BigDecimal humidityMax,

        // 생성 및 수정 시간
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // 회사 접근 권한 부여 시점
        LocalDateTime grantedAt
) {

    /**
     * ServerRoom 엔티티 + 접근 권한 시간으로 DTO 생성
     */
    public static CompanyServerRoomDetailResponse from(ServerRoom serverRoom, LocalDateTime grantedAt) {
        if (serverRoom == null) {
            throw new IllegalArgumentException("ServerRoom 엔티티가 null입니다.");
        }
        if (grantedAt == null) {
            throw new IllegalArgumentException("grantedAt 값이 null입니다.");
        }

        return CompanyServerRoomDetailResponse.builder()
                .id(serverRoom.getId())
                .name(serverRoom.getName())
                .code(serverRoom.getCode())
                .location(serverRoom.getLocation())
                .floor(serverRoom.getFloor())
                .rows(serverRoom.getRows())
                .columns(serverRoom.getColumns())
                .status(serverRoom.getStatus())
                .description(serverRoom.getDescription())
                .totalArea(serverRoom.getTotalArea())
                .totalPowerCapacity(serverRoom.getTotalPowerCapacity())
                .totalCoolingCapacity(serverRoom.getTotalCoolingCapacity())
                .currentRackCount(serverRoom.getCurrentRackCount())
                .temperatureMin(serverRoom.getTemperatureMin())
                .temperatureMax(serverRoom.getTemperatureMax())
                .humidityMin(serverRoom.getHumidityMin())
                .humidityMax(serverRoom.getHumidityMax())
                .createdAt(serverRoom.getCreatedAt())
                .updatedAt(serverRoom.getUpdatedAt())
                .grantedAt(grantedAt)
                .build();
    }
}
