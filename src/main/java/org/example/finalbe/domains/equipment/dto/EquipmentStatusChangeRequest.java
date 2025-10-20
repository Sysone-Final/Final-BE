package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;

@Builder
public record EquipmentStatusChangeRequest(
        String status,
        String reason
) {
}