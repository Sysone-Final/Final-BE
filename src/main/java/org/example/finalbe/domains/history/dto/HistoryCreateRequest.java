package org.example.finalbe.domains.history.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;


import java.util.List;
import java.util.Map;

/**
 * 히스토리 생성 요청 DTO
 * Service 레이어에서 히스토리를 기록할 때 사용
 */
@Builder
public record HistoryCreateRequest(
        Long dataCenterId,
        String dataCenterName,
        EntityType entityType,
        Long entityId,
        String entityName,
        String entityCode,
        HistoryAction action,
        Long changedBy,
        String changedByName,
        String changedByRole,
        List<String> changedFields,
        Map<String, Object> beforeValue,
        Map<String, Object> afterValue,
        String reason,
        String ipAddress,
        Map<String, Object> metadata
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JSON 문자열로 변환
     */
    public String changedFieldsAsJson() {
        if (changedFields == null || changedFields.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(changedFields);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public String beforeValueAsJson() {
        if (beforeValue == null || beforeValue.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(beforeValue);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public String afterValueAsJson() {
        if (afterValue == null || afterValue.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(afterValue);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public String metadataAsJson() {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 자동 설명 생성
     */
    public String generateDescription() {
        StringBuilder desc = new StringBuilder();

        desc.append("[").append(action.name()).append("] ");
        desc.append(entityType.name()).append(" - ").append(entityName);

        if (action == HistoryAction.STATUS_CHANGE && changedFields != null && !changedFields.isEmpty()) {
            Object before = beforeValue != null ? beforeValue.get("status") : null;
            Object after = afterValue != null ? afterValue.get("status") : null;
            if (before != null && after != null) {
                desc.append(" (").append(before).append(" → ").append(after).append(")");
            }
        } else if (action == HistoryAction.MOVE && changedFields != null) {
            Object beforeLoc = beforeValue != null ? beforeValue.get("location") : null;
            Object afterLoc = afterValue != null ? afterValue.get("location") : null;
            if (beforeLoc != null && afterLoc != null) {
                desc.append(" (").append(beforeLoc).append(" → ").append(afterLoc).append(")");
            }
        }

        if (reason != null && !reason.isEmpty()) {
            desc.append(" - 사유: ").append(reason);
        }

        return desc.toString();
    }
}