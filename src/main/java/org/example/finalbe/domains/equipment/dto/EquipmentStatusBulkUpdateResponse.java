// 작성자: 황요한
// 설명: 장비 상태 일괄 변경 요청에 대한 결과를 담는 DTO

package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record EquipmentStatusBulkUpdateResponse(
        int totalRequested,
        int successCount,
        int failureCount,
        List<Long> successIds,
        List<FailedEquipment> failedEquipments
) {

    /**
     * 상태 변경 실패 장비 정보를 담는 DTO
     */
    @Builder
    public record FailedEquipment(
            Long equipmentId,
            String equipmentName,
            String reason
    ) {
    }
}
