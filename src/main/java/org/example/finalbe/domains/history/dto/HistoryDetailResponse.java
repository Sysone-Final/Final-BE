package org.example.finalbe.domains.history.dto;

import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.history.domain.History;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 히스토리 상세 조회 응답 DTO
 * 변경된 필드들의 상세 정보를 포함하여 프론트엔드에서 표시
 */
@Builder
public record HistoryDetailResponse(
        Long id,
        Long dataCenterId,
        String dataCenterName,
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
        List<ChangeDetail> changeDetails  // 상세 변경 내역 리스트
) {
    /**
     * Entity → DTO 변환
     */
    public static HistoryDetailResponse from(History history) {
        // metadata에서 changeDetails 추출
        List<ChangeDetail> changeDetails = extractChangeDetails(history);

        return HistoryDetailResponse.builder()
                .id(history.getId())
                .dataCenterId(history.getDataCenterId())
                .dataCenterName(history.getDataCenterName())
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
                .changedFields(history.getChangedFields())
                .beforeValue(history.getBeforeValue())
                .afterValue(history.getAfterValue())
                .metadata(history.getMetadata())
                .changeDetails(changeDetails)
                .build();
    }

    /**
     * metadata에서 changeDetails 추출 및 파싱
     */
    @SuppressWarnings("unchecked")
    private static List<ChangeDetail> extractChangeDetails(History history) {
        if (history.getMetadata() == null || !history.getMetadata().containsKey("changeDetails")) {
            // changeDetails가 없으면 beforeValue/afterValue에서 직접 생성
            return buildChangeDetailsFromSnapshots(history);
        }

        try {
            Map<String, Object> changeDetailsMap = (Map<String, Object>) history.getMetadata().get("changeDetails");
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
            // 파싱 실패 시 기본 방식으로 생성
            return buildChangeDetailsFromSnapshots(history);
        }
    }

    /**
     * beforeValue/afterValue에서 changeDetails 구성
     */
    private static List<ChangeDetail> buildChangeDetailsFromSnapshots(History history) {
        if (history.getChangedFields() == null || history.getChangedFields().isEmpty()) {
            return List.of();
        }

        return history.getChangedFields().stream()
                .filter(field -> !field.equals("ALL"))
                .map(field -> {
                    Object oldValue = history.getBeforeValue() != null ?
                            history.getBeforeValue().get(field) : null;
                    Object newValue = history.getAfterValue() != null ?
                            history.getAfterValue().get(field) : null;

                    String oldValueStr = oldValue != null ? oldValue.toString() : "(없음)";
                    String newValueStr = newValue != null ? newValue.toString() : "(없음)";

                    return ChangeDetail.builder()
                            .field(field)
                            .fieldLabel(field)
                            .oldValue(oldValueStr)
                            .newValue(newValueStr)
                            .changeDescription(String.format("%s: %s → %s", field, oldValueStr, newValueStr))
                            .build();
                })
                .toList();
    }

    /**
     * 엔티티 타입 한글 변환
     */
    private static String translateEntityType(EntityType type) {
        return switch (type) {
            case DATACENTER -> "전산실";
            case RACK -> "랙";
            case EQUIPMENT -> "장비";
            case DEVICE -> "장치";
            case MEMBER -> "회원";
            case COMPANY -> "회사";
            default -> type.name();
        };
    }

    /**
     * 액션 한글 변환
     */
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

    /**
     * 개별 변경 내역
     */
    @Builder
    public record ChangeDetail(
            String field,           // 필드명 (예: "rackLocation")
            String fieldLabel,      // 필드 한글명 (예: "랙 위치")
            String oldValue,        // 이전 값 (예: "5")
            String newValue,        // 새 값 (예: "10")
            String changeDescription // 변경 설명 (예: "랙 위치: 5 → 10")
    ) {
    }
}