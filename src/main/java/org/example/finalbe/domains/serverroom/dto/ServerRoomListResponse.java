package org.example.finalbe.domains.serverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

/**
 * 서버실 목록 조회 응답 DTO (DataCenter 정보 포함)
 */
@Builder
public record ServerRoomListResponse(
        Long id,
        String name,
        String code,
        String location,
        Integer floor,
        ServerRoomStatus status,
        Long dataCenterId,
        String dataCenterName,
        String dataCenterAddress
) {
    /**
     * Entity → DTO 변환
     */
    public static ServerRoomListResponse from(ServerRoom serverRoom) {
        if (serverRoom == null) {
            throw new IllegalArgumentException("ServerRoom 엔티티가 null입니다.");
        }

        return ServerRoomListResponse.builder()
                .id(serverRoom.getId())
                .name(serverRoom.getName())
                .code(serverRoom.getCode())
                .location(serverRoom.getLocation())
                .floor(serverRoom.getFloor())
                .status(serverRoom.getStatus())
                .dataCenterId(serverRoom.getDataCenter() != null ? serverRoom.getDataCenter().getId() : null)
                .dataCenterName(serverRoom.getDataCenter() != null ? serverRoom.getDataCenter().getName() : null)
                .dataCenterAddress(serverRoom.getDataCenter() != null ? serverRoom.getDataCenter().getAddress() : null)
                .build();
    }
}