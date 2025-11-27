/**
 * 작성자: 황요한
 * 회사가 접근 권한을 가진 서버실 목록 응답 DTO
 */
package org.example.finalbe.domains.company.dto;

import lombok.Builder;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.time.LocalDateTime;

@Builder
public record CompanyServerRoomListResponse(
        Long serverRoomId,
        String serverRoomCode,
        String serverRoomName,
        String location,
        LocalDateTime grantedAt // 회사가 이 서버실 접근 권한을 부여받은 시간
) {

    /**
     * ServerRoom 엔티티 + 매핑된 시간으로 DTO 생성
     */
    public static CompanyServerRoomListResponse from(ServerRoom serverRoom, LocalDateTime grantedAt) {
        if (serverRoom == null) {
            throw new IllegalArgumentException("ServerRoom 엔티티가 null입니다.");
        }
        if (grantedAt == null) {
            throw new IllegalArgumentException("grantedAt 값이 null입니다.");
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
