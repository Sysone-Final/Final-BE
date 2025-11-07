package org.example.finalbe.domains.serverroom.dto;

import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 서버실 상세 조회 응답 DTO
 */
@Builder
public record ServerRoomDetailResponse(
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
        LocalDateTime updatedAt
) {
    /**
     * Entity → DTO 변환
     */
    public static ServerRoomDetailResponse from(ServerRoom serverRoom) {
        return ServerRoomDetailResponse.builder()
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
                .build();
    }
}