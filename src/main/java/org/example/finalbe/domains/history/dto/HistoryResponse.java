package org.example.finalbe.domains.history.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.history.domain.History;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 히스토리 응답 DTO
 */
@Builder
public record HistoryResponse(
        Long id,
        Long serverRoomId,
        String serverRoomName,
        EntityType entityType,
        Long entityId,
        String entityName,
        String entityCode,
        HistoryAction action,
        Long changedBy,
        String changedByName,
        String changedByRole,
        LocalDateTime changedAt,
        List<String> changedFields,
        JsonNode beforeValue,
        JsonNode afterValue,
        String description
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static HistoryResponse from(History history) {
        return HistoryResponse.builder()
                .id(history.getId())
                .serverRoomId(history.getServerRoomId())
                .serverRoomName(history.getServerRoomName())
                .entityType(history.getEntityType())
                .entityId(history.getEntityId())
                .entityName(history.getEntityName())
                .entityCode(history.getEntityCode())
                .action(history.getAction())
                .changedBy(history.getChangedBy())
                .changedByName(history.getChangedByName())
                .changedByRole(history.getChangedByRole())
                .changedAt(history.getChangedAt())
                .changedFields(parseJsonArray(history.getChangedFields()))
                .beforeValue(parseJsonObject(history.getBeforeValue()))
                .afterValue(parseJsonObject(history.getAfterValue()))
                .description(history.getDescription())
                .build();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static JsonNode parseJsonObject(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}