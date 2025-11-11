package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.time.LocalDateTime;

/**
 * 회사의 서버실 목록 조회 응답 DTO
 */
@Builder
public record CompanyServerRoomListResponse(
        Long serverRoomId,
        String serverRoomCode,
        String serverRoomName,
        String location,
        LocalDateTime grantedAt
) {
    /**
     * ServerRoom과 매핑 시간으로 DTO 생성
     */
    public static CompanyServerRoomListResponse from(ServerRoom serverRoom, LocalDateTime grantedAt) {
        if (serverRoom == null) {
            throw new IllegalArgumentException("ServerRoom 엔티티가 null입니다.");
        }
        if (grantedAt == null) {
            throw new IllegalArgumentException("grantedAt이 null입니다.");
        }

        return CompanyServerRoomListResponse.builder()
                .serverRoomId(serverRoom.getId())
                .serverRoomCode(serverRoom.getCode())
                .serverRoomName(serverRoom.getName())
                .location(serverRoom.getLocation())
                .grantedAt(grantedAt)
                .build();
    }
}