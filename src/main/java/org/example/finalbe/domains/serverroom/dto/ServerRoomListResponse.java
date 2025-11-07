package org.example.finalbe.domains.serverroom.dto;

import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.math.BigDecimal;

/**
 * 서버실 목록 조회 응답 DTO
 */
@Builder
public record ServerRoomListResponse(
        Long id,
        String name,
        String code,
        String location,
        Integer floor,
        ServerRoomStatus status,
        Integer currentRackCount,
        BigDecimal totalArea
) {
    /**
     * Entity → DTO 변환
     */
    public static ServerRoomListResponse from(ServerRoom serverRoom) {
        return ServerRoomListResponse.builder()
                .id(serverRoom.getId())
                .name(serverRoom.getName())
                .code(serverRoom.getCode())
                .location(serverRoom.getLocation())
                .floor(serverRoom.getFloor())
                .status(serverRoom.getStatus())
                .currentRackCount(serverRoom.getCurrentRackCount())
                .totalArea(serverRoom.getTotalArea())
                .build();
    }
}