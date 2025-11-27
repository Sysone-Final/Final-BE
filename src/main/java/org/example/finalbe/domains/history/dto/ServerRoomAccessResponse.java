// 작성자: 황요한
// 사용자가 접근 가능한 서버실 정보를 전달하는 DTO

package org.example.finalbe.domains.history.dto;

import lombok.Builder;

@Builder
public record ServerRoomAccessResponse(
        Long serverRoomId,
        String serverRoomName,
        String serverRoomCode,
        String location
) {
}
