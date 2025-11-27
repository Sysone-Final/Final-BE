// 작성자: 황요한
// 히스토리 상세 정보를 생성하고 JSON 데이터를 파싱하는 DTO

package org.example.finalbe.domains.history.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.history.domain.History;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Builder
public record HistoryDetailResponse(
        Long id,
        Long serverRoomId,
        String serverRoomName,
        EntityType entityType,
        String entityTypeName,
        Long entityId,
        String entityName,
        String entityCode,
        HistoryAction action,
        String actionName,
        Long changedBy,
        String changedByName,
        String changedByRole,
        LocalDateTime changedAt,
        List<String> changedFields,
        Map<String, Object> beforeValue,
        Map<String, Object> afterValue,
        Map<String, Object> metadata,
        List<ChangeDetail> changeDetails
) {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // History 엔티티를 DTO로 변환
    public static HistoryDetailResponse from(History history) {
        List<String> changedFields = parseChangedFields(history.getChangedFields());
        Map<String, Object> beforeValue = parseJsonToMap(history.getBeforeValue());
        Map<String, Object> afterValue = parseJsonToMap(history.getAfterValue());
        Map<String, Object> metadata = parseJsonToMap(history.getMetadata());

        List<ChangeDetail> changeDetails = extractChangeDetails(
                metadata, changedFields, beforeValue, afterValue
        );

        return HistoryDetailResponse.builder()
                .id(history.getId())
                .serverRoomId(history.getServerRoomId())
                .serverRoomName(history.getServerRoomName())
                .entityType(history.getEntityType())
                .entityTypeName(translateEntityType(history.getEntityType()))
                .entityId(history.getEntityId())
                .entityName(history.getEntityName())
                .entityCode(history.getEntityCode())
                .action(history.getAction())
                .actionName(translateAction(history.getAction()))
                .changedBy(history.getChangedBy())
                .changedByName(history.getChangedByName())
                .changedByRole(history.getChangedByRole())
                .changedAt(history.getChangedAt())
                .changedFields(changedFields)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .metadata(metadata)
                .changeDetails(changeDetails)
                .build();
    }

    // 변경 필드 목록 파싱
    private static List<String> parseChangedFields(Object changedFieldsObj) {
        if (changedFieldsObj == null) return new ArrayList<>();

        if (changedFieldsObj instanceof List) {
            return (List<String>) changedFieldsObj;
        }

        if (changedFieldsObj instanceof String jsonStr) {
            if (jsonStr.isEmpty()) return new ArrayList<>();
            try {
                return objectMapper.readValue(jsonStr, new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to parse changedFields: {}", jsonStr, e);
                return new ArrayList<>();
            }
        }

        return new ArrayList<>();
    }

    // JSON 문자열을 Map으로 변환
    private static Map<String, Object> parseJsonToMap(Object jsonObj) {
        if (jsonObj == null) return new HashMap<>();

        if (jsonObj instanceof Map) {
            return (Map<String, Object>) jsonObj;
        }

        if (jsonObj instanceof String jsonStr) {
            if (jsonStr.isEmpty()) return new HashMap<>();
            try {
                return objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                log.error("Failed to parse JSON to Map: {}", jsonStr, e);
                return new HashMap<>();
            }
        }

        return new HashMap<>();
    }

    // metadata 기반 changeDetails 생성
    @SuppressWarnings("unchecked")
    private static List<ChangeDetail> extractChangeDetails(
            Map<String, Object> metadata,
            List<String> changedFields,
            Map<String, Object> beforeValue,
            Map<String, Object> afterValue
    ) {
        if (metadata == null || !metadata.containsKey("changeDetails")) {
            return buildChangeDetailsFromSnapshots(changedFields, beforeValue, afterValue);
        }

        try {
            Map<String, Object> changeDetailsMap = (Map<String, Object>) metadata.get("changeDetails");

            return changeDetailsMap.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> detail = (Map<String, Object>) entry.getValue();
                        return ChangeDetail.builder()
                                .field(entry.getKey())
                                .fieldLabel((String) detail.get("fieldLabel"))
                                .oldValue((String) detail.get("oldValue"))
                                .newValue((String) detail.get("newValue"))
                                .changeDescription((String) detail.get("changeDescription"))
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse changeDetails from metadata", e);
            return buildChangeDetailsFromSnapshots(changedFields, beforeValue, afterValue);
        }
    }

    // before/after 값으로 changeDetails 생성
    private static List<ChangeDetail> buildChangeDetailsFromSnapshots(
            List<String> changedFields,
            Map<String, Object> beforeValue,
            Map<String, Object> afterValue
    ) {
        if (changedFields == null || changedFields.isEmpty()) {
            return List.of();
        }

        return changedFields.stream()
                .filter(field -> !field.equals("ALL"))
                .map(field -> {
                    Object oldValue = beforeValue.get(field);
                    Object newValue = afterValue.get(field);

                    String oldStr = oldValue != null ? oldValue.toString() : "(없음)";
                    String newStr = newValue != null ? newValue.toString() : "(없음)";

                    return ChangeDetail.builder()
                            .field(field)
                            .fieldLabel(field)
                            .oldValue(oldStr)
                            .newValue(newStr)
                            .changeDescription(field + ": " + oldStr + " → " + newStr)
                            .build();
                })
                .toList();
    }

    // 엔티티 타입 한글 변환
    private static String translateEntityType(EntityType type) {
        return switch (type) {
            case SERVERROOM -> "전산실";
            case RACK -> "랙";
            case EQUIPMENT -> "장비";
            case DEVICE -> "장치";
            case MEMBER -> "회원";
            case COMPANY -> "회사";
            default -> type.name();
        };
    }

    // 액션 한글 변환
    private static String translateAction(HistoryAction action) {
        return switch (action) {
            case CREATE -> "생성";
            case UPDATE -> "수정";
            case DELETE -> "삭제";
            case STATUS_CHANGE -> "상태 변경";
            case MOVE -> "이동";
            default -> action.name();
        };
    }

    @Builder
    public record ChangeDetail(
            String field,
            String fieldLabel,
            String oldValue,
            String newValue,
            String changeDescription
    ) {
    }
}
