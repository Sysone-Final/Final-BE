package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.history.dto.HistoryCreateRequest;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.member.domain.Member;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Equipment 히스토리 기록 전담 클래스 (개선 버전)
 * 변경 내역을 상세하게 기록하여 "무엇이 어떻게 변경되었는지" 추적 가능
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EquipmentHistoryRecorder {

    private final HistoryService historyService;

    /**
     * Equipment 생성 히스토리
     */
    public void recordCreate(Equipment equipment, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(equipment.getRack().getServerRoom().getId())
                .serverRoomName(equipment.getRack().getServerRoom().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(equipment.getId())
                .entityName(equipment.getName())
                .entityCode(equipment.getCode())
                .action(HistoryAction.CREATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .afterValue(buildSnapshot(equipment))
                .metadata(Map.of(
                        "rackName", equipment.getRack().getRackName(),
                        "rackId", String.valueOf(equipment.getRack().getId())
                ))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Equipment 수정 히스토리 (상세 변경 내역 포함)
     */
    public void recordUpdate(Equipment oldEquipment, Equipment newEquipment, Member member) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldEquipment);
        Map<String, Object> newSnapshot = buildSnapshot(newEquipment);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            log.info("No changes detected for equipment id: {}", newEquipment.getId());
            return;
        }

        // 변경 내역 상세 정보 구성
        Map<String, Object> changeDetails = buildChangeDetails(oldSnapshot, newSnapshot, changedFields);

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(newEquipment.getRack().getServerRoom().getId())
                .serverRoomName(newEquipment.getRack().getServerRoom().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(newEquipment.getId())
                .entityName(newEquipment.getName())
                .entityCode(newEquipment.getCode())
                .action(HistoryAction.UPDATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(changedFields)
                .beforeValue(oldSnapshot)
                .afterValue(newSnapshot)
                .metadata(Map.of(
                        "changeDetails", changeDetails,
                        "rackName", newEquipment.getRack().getRackName(),
                        "rackId", String.valueOf(newEquipment.getRack().getId())
                ))
                .build();

        historyService.recordHistory(request);
        log.info("Equipment update history recorded: {} fields changed", changedFields.size());
    }

    /**
     * Equipment 이동 히스토리
     */
    public void recordMove(Equipment equipment, String oldLocation, String newLocation, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(equipment.getRack().getServerRoom().getId())
                .serverRoomName(equipment.getRack().getServerRoom().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(equipment.getId())
                .entityName(equipment.getName())
                .entityCode(equipment.getCode())
                .action(HistoryAction.MOVE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("startUnit"))
                .beforeValue(Map.of("location", oldLocation))
                .afterValue(Map.of("location", newLocation))
                .metadata(Map.of(
                        "rackName", equipment.getRack().getRackName(),
                        "rackId", String.valueOf(equipment.getRack().getId()),
                        "oldLocation", oldLocation,
                        "newLocation", newLocation
                ))
                .build();

        historyService.recordHistory(request);
        log.info("Equipment move history recorded: {} -> {}", oldLocation, newLocation);
    }
    /**
     * Equipment 상태 변경 히스토리
     */
    public void recordStatusChange(Equipment equipment, String oldStatus, String newStatus, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(equipment.getRack().getServerRoom().getId())
                .serverRoomName(equipment.getRack().getServerRoom().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(equipment.getId())
                .entityName(equipment.getName())
                .entityCode(equipment.getCode())
                .action(HistoryAction.STATUS_CHANGE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("status"))
                .beforeValue(Map.of("status", oldStatus))
                .afterValue(Map.of("status", newStatus))
                .metadata(Map.of(
                        "rackName", equipment.getRack().getRackName(),
                        "rackId", String.valueOf(equipment.getRack().getId())
                ))
                .build();

        historyService.recordHistory(request);
        log.info("Equipment status change history recorded: {} -> {}", oldStatus, newStatus);
    }

    /**
     * Equipment 삭제 히스토리
     */
    public void recordDelete(Equipment equipment, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(equipment.getRack().getServerRoom().getId())
                .serverRoomName(equipment.getRack().getServerRoom().getName())
                .entityType(EntityType.EQUIPMENT)
                .entityId(equipment.getId())
                .entityName(equipment.getName())
                .entityCode(equipment.getCode())
                .action(HistoryAction.DELETE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .beforeValue(buildSnapshot(equipment))
                .metadata(Map.of(
                        "rackName", equipment.getRack().getRackName(),
                        "rackId", String.valueOf(equipment.getRack().getId())
                ))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Equipment 상태 스냅샷 생성
     */
    private Map<String, Object> buildSnapshot(Equipment equipment) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name", equipment.getName());
        snapshot.put("code", equipment.getCode());
        snapshot.put("type", equipment.getType() != null ? equipment.getType().name() : null);
        snapshot.put("rackName", equipment.getRack().getRackName());
        snapshot.put("startUnit", equipment.getStartUnit());
        snapshot.put("unitSize", equipment.getUnitSize());
        snapshot.put("status", equipment.getStatus() != null ? equipment.getStatus().name() : null);
        snapshot.put("modelName", equipment.getModelName());
        snapshot.put("manufacturer", equipment.getManufacturer());
        snapshot.put("serialNumber", equipment.getSerialNumber());
        snapshot.put("ipAddress", equipment.getIpAddress());
        snapshot.put("powerConsumption", equipment.getPowerConsumption());
        return snapshot;
    }

    private List<String> detectChangedFields(Map<String, Object> oldSnapshot, Map<String, Object> newSnapshot) {
        List<String> changedFields = new ArrayList<>();

        for (String key : newSnapshot.keySet()) {
            Object oldValue = oldSnapshot.get(key);
            Object newValue = newSnapshot.get(key);

            if (oldValue == null && newValue != null) {
                changedFields.add(key);
            } else if (oldValue != null && !oldValue.equals(newValue)) {
                changedFields.add(key);
            }
        }

        return changedFields;
    }

    /**
     * 변경 내역 상세 정보 구성
     */
    private Map<String, Object> buildChangeDetails(
            Map<String, Object> oldSnapshot,
            Map<String, Object> newSnapshot,
            List<String> changedFields) {

        Map<String, Object> changeDetails = new HashMap<>();

        for (String field : changedFields) {
            Object oldValue = oldSnapshot.get(field);
            Object newValue = newSnapshot.get(field);

            // 필드별 한글 레이블 매핑
            String fieldLabel = getFieldLabel(field);

            // 변경 전/후 값을 사용자 친화적 형태로 변환
            String oldValueStr = formatValue(field, oldValue);
            String newValueStr = formatValue(field, newValue);

            changeDetails.put(field, Map.of(
                    "fieldLabel", fieldLabel,
                    "oldValue", oldValueStr,
                    "newValue", newValueStr,
                    "changeDescription", String.format("%s: %s → %s", fieldLabel, oldValueStr, newValueStr)
            ));
        }

        return changeDetails;
    }

    /**
     * 필드명을 한글 레이블로 변환
     */
    private String getFieldLabel(String field) {
        return switch (field) {
            case "name" -> "장비명";
            case "code" -> "장비 코드";
            case "type" -> "장비 타입";
            case "rackName" -> "랙 이름";
            case "startUnit" -> "시작 유닛";
            case "unitSize" -> "유닛 크기";
            case "status" -> "상태";
            case "modelName" -> "모델명";
            case "manufacturer" -> "제조사";
            case "serialNumber" -> "시리얼 번호";
            case "ipAddress" -> "IP 주소";
            case "macAddress" -> "MAC 주소";
            case "powerConsumption" -> "전력 소비량";
            default -> field;
        };
    }

    private String formatValue(String field, Object value) {
        if (value == null) {
            return "(없음)";
        }

        return switch (field) {
            case "startUnit", "unitSize" -> value + " U";
            case "powerConsumption" -> value + " W";
            case "status" -> translateStatus(value.toString());
            default -> value.toString();
        };
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "NORMAL" -> "정상";
            case "WARNING" -> "경고";
            case "ERROR" -> "오류";
            case "MAINTENANCE" -> "점검중";
            default -> status;
        };
    }
}