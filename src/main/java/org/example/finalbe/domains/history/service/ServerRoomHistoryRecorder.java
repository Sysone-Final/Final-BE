package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.history.dto.HistoryCreateRequest;
import org.example.finalbe.domains.member.domain.Member;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ServerRoom 히스토리 기록 전담 클래스 (개선 버전)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ServerRoomHistoryRecorder {

    private final HistoryService historyService;

    /**
     * ServerRoom 생성 히스토리
     */
    public void recordCreate(ServerRoom serverRoom, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(serverRoom.getId())
                .serverRoomName(serverRoom.getName())
                .entityType(EntityType.SERVERROOM)
                .entityId(serverRoom.getId())
                .entityName(serverRoom.getName())
                .entityCode(serverRoom.getCode())
                .action(HistoryAction.CREATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .afterValue(buildSnapshot(serverRoom))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * ServerRoom 수정 히스토리 (상세 변경 내역 포함)
     */
    public void recordUpdate(ServerRoom oldServerRoom, ServerRoom newServerRoom, Member member) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldServerRoom);
        Map<String, Object> newSnapshot = buildSnapshot(newServerRoom);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            log.info("No changes detected for serverroom id: {}", newServerRoom.getId());
            return;
        }

        // 변경 내역 상세 정보 구성
        Map<String, Object> changeDetails = buildChangeDetails(oldSnapshot, newSnapshot, changedFields);

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(newServerRoom.getId())
                .serverRoomName(newServerRoom.getName())
                .entityType(EntityType.SERVERROOM)
                .entityId(newServerRoom.getId())
                .entityName(newServerRoom.getName())
                .entityCode(newServerRoom.getCode())
                .action(HistoryAction.UPDATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(changedFields)
                .beforeValue(oldSnapshot)
                .afterValue(newSnapshot)
                .metadata(Map.of("changeDetails", changeDetails))
                .build();

        historyService.recordHistory(request);
        log.info("ServerRoom update history recorded: {} fields changed", changedFields.size());
    }

    /**
     * ServerRoom 상태 변경 히스토리
     */
    public void recordStatusChange(ServerRoom serverRoom, String oldStatus, String newStatus, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(serverRoom.getId())
                .serverRoomName(serverRoom.getName())
                .entityType(EntityType.SERVERROOM)
                .entityId(serverRoom.getId())
                .entityName(serverRoom.getName())
                .entityCode(serverRoom.getCode())
                .action(HistoryAction.STATUS_CHANGE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("status"))
                .beforeValue(Map.of("status", oldStatus))
                .afterValue(Map.of("status", newStatus))
                .build();

        historyService.recordHistory(request);
        log.info("ServerRoom status change history recorded: {} -> {}", oldStatus, newStatus);
    }

    /**
     * ServerRoom 삭제 히스토리
     */
    public void recordDelete(ServerRoom serverRoom, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(serverRoom.getId())
                .serverRoomName(serverRoom.getName())
                .entityType(EntityType.SERVERROOM)
                .entityId(serverRoom.getId())
                .entityName(serverRoom.getName())
                .entityCode(serverRoom.getCode())
                .action(HistoryAction.DELETE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .beforeValue(buildSnapshot(serverRoom))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * ServerRoom 상태 스냅샷 생성
     */
    private Map<String, Object> buildSnapshot(ServerRoom serverRoom) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name", serverRoom.getName());
        snapshot.put("code", serverRoom.getCode());
        snapshot.put("location", serverRoom.getLocation());
        snapshot.put("floor", serverRoom.getFloor());
        snapshot.put("rows", serverRoom.getRows());
        snapshot.put("columns", serverRoom.getColumns());
        snapshot.put("status", serverRoom.getStatus() != null ? serverRoom.getStatus().name() : null);
        snapshot.put("description", serverRoom.getDescription());
        snapshot.put("totalArea", serverRoom.getTotalArea());
        snapshot.put("totalPowerCapacity", serverRoom.getTotalPowerCapacity());
        snapshot.put("totalCoolingCapacity", serverRoom.getTotalCoolingCapacity());
        snapshot.put("currentRackCount", serverRoom.getCurrentRackCount());
        snapshot.put("temperatureMin", serverRoom.getTemperatureMin());
        snapshot.put("temperatureMax", serverRoom.getTemperatureMax());
        snapshot.put("humidityMin", serverRoom.getHumidityMin());
        snapshot.put("humidityMax", serverRoom.getHumidityMax());
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

            String fieldLabel = getFieldLabel(field);
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

    private String getFieldLabel(String field) {
        return switch (field) {
            case "name" -> "전산실 이름";
            case "code" -> "전산실 코드";
            case "location" -> "위치";
            case "floor" -> "층";
            case "rows" -> "행 수";
            case "columns" -> "열 수";
            case "status" -> "상태";
            case "description" -> "설명";
            case "totalArea" -> "총 면적";
            case "totalPowerCapacity" -> "총 전력 용량";
            case "totalCoolingCapacity" -> "총 냉각 용량";
            case "currentRackCount" -> "현재 랙 개수";
            case "temperatureMin" -> "최저 온도";
            case "temperatureMax" -> "최고 온도";
            case "humidityMin" -> "최저 습도";
            case "humidityMax" -> "최고 습도";
            default -> field;
        };
    }

    private String formatValue(String field, Object value) {
        if (value == null) {
            return "(없음)";
        }

        return switch (field) {
            case "totalArea" -> value + " m²";
            case "totalPowerCapacity", "totalCoolingCapacity" -> value + " kW";
            case "temperatureMin", "temperatureMax" -> value + " ℃";
            case "humidityMin", "humidityMax" -> value + " %";
            case "status" -> translateStatus(value.toString());
            default -> value.toString();
        };
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "ACTIVE" -> "활성";
            case "INACTIVE" -> "비활성";
            case "MAINTENANCE" -> "점검중";
            case "PLANNING" -> "계획중";
            default -> status;
        };
    }
}