// 작성자: 황요한
// 장비(Equipment)의 변경 이력을 기록하는 서비스 클래스

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

@Component
@Slf4j
@RequiredArgsConstructor
public class EquipmentHistoryRecorder {

    private final HistoryService historyService;

    // 장비 생성 이력을 기록
    public void recordCreate(Equipment equipment, Member member) {
        Long serverRoomId = null;
        String serverRoomName = "미배정";
        Map<String, Object> metadata = new HashMap<>();

        if (equipment.getRack() != null) {
            metadata.put("rackName", equipment.getRack().getRackName());
            metadata.put("rackId", String.valueOf(equipment.getRack().getId()));

            if (equipment.getRack().getServerRoom() != null) {
                serverRoomId = equipment.getRack().getServerRoom().getId();
                serverRoomName = equipment.getRack().getServerRoom().getName();
            }
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(serverRoomId)
                .serverRoomName(serverRoomName)
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
                .metadata(metadata)
                .build();

        historyService.recordHistory(request);
    }

    // 장비 수정 이력을 기록
    public void recordUpdate(Equipment oldEquipment, Equipment newEquipment, Member member) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldEquipment);
        Map<String, Object> newSnapshot = buildSnapshot(newEquipment);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            return;
        }

        Map<String, Object> changeDetails = buildChangeDetails(oldSnapshot, newSnapshot, changedFields);

        Long serverRoomId = null;
        String serverRoomName = "미배정";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("changeDetails", changeDetails);

        if (newEquipment.getRack() != null) {
            metadata.put("rackName", newEquipment.getRack().getRackName());
            metadata.put("rackId", String.valueOf(newEquipment.getRack().getId()));

            if (newEquipment.getRack().getServerRoom() != null) {
                serverRoomId = newEquipment.getRack().getServerRoom().getId();
                serverRoomName = newEquipment.getRack().getServerRoom().getName();
            }
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(serverRoomId)
                .serverRoomName(serverRoomName)
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
                .metadata(metadata)
                .build();

        historyService.recordHistory(request);
    }

    // 장비 위치 이동 이력을 기록
    public void recordMove(Equipment equipment, String oldLocation, String newLocation, Member member) {
        Long serverRoomId = null;
        String serverRoomName = "미배정";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("oldLocation", oldLocation);
        metadata.put("newLocation", newLocation);

        if (equipment.getRack() != null) {
            metadata.put("rackName", equipment.getRack().getRackName());
            metadata.put("rackId", String.valueOf(equipment.getRack().getId()));

            if (equipment.getRack().getServerRoom() != null) {
                serverRoomId = equipment.getRack().getServerRoom().getId();
                serverRoomName = equipment.getRack().getServerRoom().getName();
            }
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(serverRoomId)
                .serverRoomName(serverRoomName)
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
                .metadata(metadata)
                .build();

        historyService.recordHistory(request);
    }

    // 장비 상태 변경 이력을 기록
    public void recordStatusChange(Equipment equipment, String oldStatus, String newStatus, Member member) {
        Long serverRoomId = null;
        String serverRoomName = "미배정";
        Map<String, Object> metadata = new HashMap<>();

        if (equipment.getRack() != null) {
            metadata.put("rackName", equipment.getRack().getRackName());
            metadata.put("rackId", String.valueOf(equipment.getRack().getId()));

            if (equipment.getRack().getServerRoom() != null) {
                serverRoomId = equipment.getRack().getServerRoom().getId();
                serverRoomName = equipment.getRack().getServerRoom().getName();
            }
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(serverRoomId)
                .serverRoomName(serverRoomName)
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
                .metadata(metadata)
                .build();

        historyService.recordHistory(request);
    }

    // 장비 삭제 이력을 기록
    public void recordDelete(Equipment equipment, Member member) {
        Long serverRoomId = null;
        String serverRoomName = "미배정";
        Map<String, Object> metadata = new HashMap<>();

        if (equipment.getRack() != null) {
            metadata.put("rackName", equipment.getRack().getRackName());
            metadata.put("rackId", String.valueOf(equipment.getRack().getId()));

            if (equipment.getRack().getServerRoom() != null) {
                serverRoomId = equipment.getRack().getServerRoom().getId();
                serverRoomName = equipment.getRack().getServerRoom().getName();
            }
        }

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(serverRoomId)
                .serverRoomName(serverRoomName)
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
                .metadata(metadata)
                .build();

        historyService.recordHistory(request);
    }

    // 장비 상태 스냅샷을 생성
    private Map<String, Object> buildSnapshot(Equipment equipment) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name", equipment.getName());
        snapshot.put("code", equipment.getCode());
        snapshot.put("type", equipment.getType() != null ? equipment.getType().name() : null);

        if (equipment.getRack() != null) {
            snapshot.put("rackName", equipment.getRack().getRackName());
            snapshot.put("rackId", equipment.getRack().getId());
        } else {
            snapshot.put("rackName", null);
            snapshot.put("rackId", null);
        }

        snapshot.put("startUnit", equipment.getStartUnit());
        snapshot.put("unitSize", equipment.getUnitSize());
        snapshot.put("status", equipment.getStatus() != null ? equipment.getStatus().name() : null);
        snapshot.put("modelName", equipment.getModelName());
        snapshot.put("manufacturer", equipment.getManufacturer());
        snapshot.put("serialNumber", equipment.getSerialNumber());
        snapshot.put("ipAddress", equipment.getIpAddress());
        return snapshot;
    }

    // 변경된 필드를 추출
    private List<String> detectChangedFields(Map<String, Object> oldSnapshot, Map<String, Object> newSnapshot) {
        List<String> changedFields = new ArrayList<>();
        for (String key : newSnapshot.keySet()) {
            Object oldValue = oldSnapshot.get(key);
            Object newValue = newSnapshot.get(key);
            if (oldValue == null && newValue != null || oldValue != null && !oldValue.equals(newValue)) {
                changedFields.add(key);
            }
        }
        return changedFields;
    }

    // 변경 상세 정보를 구성
    private Map<String, Object> buildChangeDetails(
            Map<String, Object> oldSnapshot,
            Map<String, Object> newSnapshot,
            List<String> changedFields) {

        Map<String, Object> changeDetails = new HashMap<>();

        for (String field : changedFields) {
            String fieldLabel = getFieldLabel(field);
            String oldValueStr = formatValue(field, oldSnapshot.get(field));
            String newValueStr = formatValue(field, newSnapshot.get(field));

            changeDetails.put(field, Map.of(
                    "fieldLabel", fieldLabel,
                    "oldValue", oldValueStr,
                    "newValue", newValueStr,
                    "changeDescription", fieldLabel + ": " + oldValueStr + " → " + newValueStr
            ));
        }

        return changeDetails;
    }

    // 필드명 → 한글 레이블 변환
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
            default -> field;
        };
    }

    // 필드값 표시 형식 변환
    private String formatValue(String field, Object value) {
        if (value == null) return "(없음)";
        return switch (field) {
            case "startUnit", "unitSize" -> value + " U";
            case "powerConsumption" -> value + " W";
            case "status" -> translateStatus(value.toString());
            default -> value.toString();
        };
    }

    // 상태값 → 한글 변환
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
