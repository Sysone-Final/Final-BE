// 작성자: 황요한
// 설명: 서버실의 기본 정보를 전달하는 DTO (id, 이름, 행/열 정보 포함)

package org.example.finalbe.domains.device.dto;

import lombok.Builder;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

@Builder
public record ServerRoomInfo(
        Long id,        // 서버실 ID
        String name,    // 서버실 이름
        Integer rows,   // 서버실 행 수
        Integer columns // 서버실 열 수
) {

    // ServerRoom 엔티티를 DTO로 변환하는 역할
    public static ServerRoomInfo from(ServerRoom serverRoom) {
        return ServerRoomInfo.builder()
                .id(serverRoom.getId())
                .name(serverRoom.getName())
                .rows(serverRoom.getRows())
                .columns(serverRoom.getColumns())
                .build();
    }
}
