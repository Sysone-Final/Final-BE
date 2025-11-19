package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

/**
 * 서버실 정보 DTO
 */
@Builder
public record ServerRoomInfo(
        Long id,
        String name,
        Integer rows,
        Integer columns
) {
    public static ServerRoomInfo from(ServerRoom serverRoom) {
        return ServerRoomInfo.builder()
                .id(serverRoom.getId())
                .name(serverRoom.getName())
                .rows(serverRoom.getRows())
                .columns(serverRoom.getColumns())
                .build();
    }
}