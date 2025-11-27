/**
 * 작성자: 황요한
 * 데이터센터별 서버실 목록 응답 DTO
 */
package org.example.finalbe.domains.serverroom.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;

import java.util.List;

@Builder
public record ServerRoomGroupedByDataCenterResponse(
        Long dataCenterId,       // 데이터센터 ID
        String dataCenterName,   // 데이터센터 이름
        String dataCenterCode,   // 데이터센터 코드
        String dataCenterAddress,// 데이터센터 주소
        List<ServerRoomInfo> serverRooms // 서버실 목록
) {
    @Builder
    public record ServerRoomInfo(
            Long id,                 // 서버실 ID
            String name,             // 서버실 이름
            String code,             // 서버실 코드
            String location,         // 위치
            Integer floor,           // 층수
            ServerRoomStatus status, // 상태
            String description       // 설명
    ) {}
}
