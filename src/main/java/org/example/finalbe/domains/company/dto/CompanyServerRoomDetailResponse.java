package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 회사의 서버실 상세 조회 응답 DTO
 * 서버실의 모든 상세 정보 + 접근 권한 부여 시간 포함
 */
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
        BigDecimal totalArea,
        BigDecimal totalPowerCapacity,
        BigDecimal totalCoolingCapacity,
        Integer currentRackCount,
        BigDecimal temperatureMin,
        BigDecimal temperatureMax,
        BigDecimal humidityMin,
        BigDecimal humidityMax,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime grantedAt  // 회사에 접근 권한이 부여된 시간
) {
    /**
     * ServerRoom 엔티티와 매핑 시간으로 DTO 생성
     */
    public static CompanyServerRoomDetailResponse from(ServerRoom serverRoom, LocalDateTime grantedAt) {
        if (serverRoom == null) {
            throw new IllegalArgumentException("ServerRoom 엔티티가 null입니다.");
        }
        if (grantedAt == null) {
            throw new IllegalArgumentException("grantedAt이 null입니다.");
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