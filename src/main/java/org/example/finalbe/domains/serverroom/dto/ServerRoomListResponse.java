/**
 * 작성자: 황요한
 * 서버실 목록 조회 응답 DTO
 */
package org.example.finalbe.domains.serverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

@Builder
public record ServerRoomListResponse(
        Long id,                  // 서버실 ID
        String name,              // 서버실 이름
        String code,              // 서버실 코드
        String location,          // 위치
        Integer floor,            // 층수
        ServerRoomStatus status,  // 상태
        Long dataCenterId,        // 데이터센터 ID
        String dataCenterName,    // 데이터센터 이름
        String dataCenterAddress  // 데이터센터 주소
) {
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
