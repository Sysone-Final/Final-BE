package org.example.finalbe.domains.history.dto;

import lombok.Builder;

/**
 * 사용자가 접근 가능한 서버실 목록 응답 DTO
 * 히스토리 조회 시 서버실 필터링용
 */
@Builder
public record ServerRoomAccessResponse(
        Long dataCenterId,
        String dataCenterName,
        String dataCenterCode,
        String location
) {
}