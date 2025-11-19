package org.example.finalbe.domains.serverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

/**
 * 데이터센터 상세 조회 시 포함되는 서버실 간단 정보 DTO
 */
@Builder
public record ServerRoomSimpleResponse(
        Long id,
        String name,
        String code,
        String location
) {
    public static ServerRoomSimpleResponse from(ServerRoom serverRoom) {
        if (serverRoom == null) {
            return null;
        }

        return ServerRoomSimpleResponse.builder()
                .id(serverRoom.getId())
                .name(serverRoom.getName())
                .code(serverRoom.getCode())
                .location(serverRoom.getLocation())
                .build();
    }
}