package org.example.finalbe.domains.equipment.dto;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 장비 페이지네이션 응답 DTO
 */
@Builder
public record EquipmentPageResponse(
        List<EquipmentListResponse> content,
        int totalElements,
        int totalPages,
        boolean last,
        int size,
        int number
) {
    public static EquipmentPageResponse from(Page<EquipmentListResponse> page) {
        return EquipmentPageResponse.builder()
                .content(page.getContent())
                .totalElements((int) page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .size(page.getSize())
                .number(page.getNumber())
                .build();
    }
}