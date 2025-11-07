package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;

import java.util.List;

/**
 * 장비 대량 상태 변경 응답 DTO
 */
@Builder
public record EquipmentStatusBulkUpdateResponse(
        int totalRequested,      // 요청된 총 장비 수
        int successCount,        // 성공한 장비 수
        int failureCount,        // 실패한 장비 수
        List<Long> successIds,   // 성공한 장비 ID 목록
        List<FailedEquipment> failedEquipments  // 실패한 장비 상세 정보
) {
    @Builder
    public record FailedEquipment(
            Long equipmentId,
            String equipmentName,
            String reason
    ) {
    }
}