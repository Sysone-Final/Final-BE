/**
 * 작성자: 황요한
 * Rack 히스토리 기록 전담 클래스
 */
package org.example.finalbe.domains.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.history.dto.HistoryCreateRequest;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.rack.domain.Rack;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RackHistoryRecorder {

    private final HistoryService historyService;

    /**
     * Rack 생성 히스토리
     */
    public void recordCreate(Rack rack, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(rack.getServerRoom().getId())
                .serverRoomName(rack.getServerRoom().getName())
                .entityType(EntityType.RACK)
                .entityId(rack.getId())
                .entityName(rack.getRackName())
                .entityCode(rack.getRackName())
                .action(HistoryAction.CREATE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .afterValue(buildSnapshot(rack))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Rack 수정 히스토리
     */
    public void recordUpdate(Rack oldRack, Rack newRack, Member member) {
        Map<String, Object> oldSnapshot = buildSnapshot(oldRack);
        Map<String, Object> newSnapshot = buildSnapshot(newRack);
        List<String> changedFields = detectChangedFields(oldSnapshot, newSnapshot);

        if (changedFields.isEmpty()) {
            log.info("No changes detected for rack id: {}", newRack.getId());
            return;
        }

        Map<String, Object> changeDetails = buildChangeDetails(oldSnapshot, newSnapshot, changedFields);

        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(newRack.getServerRoom().getId())
                .serverRoomName(newRack.getServerRoom().getName())
                .entityType(EntityType.RACK)
                .entityId(newRack.getId())
                .entityName(newRack.getRackName())
                .entityCode(newRack.getRackName())
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
        log.info("Rack update history recorded: {} fields changed", changedFields.size());
    }

    /**
     * Rack 상태 변경 히스토리
     */
    public void recordStatusChange(Rack rack, String oldStatus, String newStatus, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(rack.getServerRoom().getId())
                .serverRoomName(rack.getServerRoom().getName())
                .entityType(EntityType.RACK)
                .entityId(rack.getId())
                .entityName(rack.getRackName())
                .entityCode(rack.getRackName())
                .action(HistoryAction.STATUS_CHANGE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("status"))
                .beforeValue(Map.of("status", oldStatus))
                .afterValue(Map.of("status", newStatus))
                .build();

        historyService.recordHistory(request);
        log.info("Rack status change history recorded: {} -> {}", oldStatus, newStatus);
    }

    /**
     * Rack 삭제 히스토리
     */
    public void recordDelete(Rack rack, Member member) {
        HistoryCreateRequest request = HistoryCreateRequest.builder()
                .serverRoomId(rack.getServerRoom().getId())
                .serverRoomName(rack.getServerRoom().getName())
                .entityType(EntityType.RACK)
                .entityId(rack.getId())
                .entityName(rack.getRackName())
                .entityCode(rack.getRackName())
                .action(HistoryAction.DELETE)
                .changedBy(member.getId())
                .changedByName(member.getName())
                .changedByRole(member.getRole().name())
                .changedFields(List.of("ALL"))
                .beforeValue(buildSnapshot(rack))
                .build();

        historyService.recordHistory(request);
    }

    /**
     * Rack 상태 스냅샷 생성
     */
    private Map<String, Object> buildSnapshot(Rack rack) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("rackName", rack.getRackName());
        snapshot.put("gridX", rack.getGridX());
        snapshot.put("gridY", rack.getGridY());
        snapshot.put("totalUnits", rack.getTotalUnits());
        snapshot.put("usedUnits", rack.getUsedUnits());
        snapshot.put("availableUnits", rack.getAvailableUnits());
        snapshot.put("status", rack.getStatus() != null ? rack.getStatus().name() : null);
        snapshot.put("rackType", rack.getRackType() != null ? rack.getRackType().name() : null);
        snapshot.put("doorDirection", rack.getDoorDirection() != null ? rack.getDoorDirection().name() : null);
        snapshot.put("zoneDirection", rack.getZoneDirection() != null ? rack.getZoneDirection().name() : null);
        snapshot.put("maxPowerCapacity", rack.getMaxPowerCapacity());
        snapshot.put("currentPowerUsage", rack.getCurrentPowerUsage());
        snapshot.put("measuredPower", rack.getMeasuredPower());
        snapshot.put("manufacturer", rack.getManufacturer());
        snapshot.put("serialNumber", rack.getSerialNumber());
        snapshot.put("notes", rack.getNotes());
        return snapshot;
    }

    /**
     * 변경된 필드 감지
     */
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

    /**
     * 필드 라벨 변환
     */
    private String getFieldLabel(String field) {
        return switch (field) {
            case "rackName" -> "랙 이름";
            case "gridX" -> "X 좌표";
            case "gridY" -> "Y 좌표";
            case "totalUnits" -> "총 유닛 수";
            case "usedUnits" -> "사용 유닛 수";
            case "availableUnits" -> "가용 유닛 수";
            case "status" -> "상태";
            case "rackType" -> "랙 타입";
            case "doorDirection" -> "도어 방향";
            case "zoneDirection" -> "존 방향";
            case "maxPowerCapacity" -> "최대 전력 용량";
            case "currentPowerUsage" -> "현재 전력 사용량";
            case "measuredPower" -> "실측 전력";
            case "manufacturer" -> "제조사";
            case "serialNumber" -> "시리얼 번호";
            case "notes" -> "비고";
            default -> field;
        };
    }

    /**
     * 값 포맷팅
     */
    private String formatValue(String field, Object value) {
        if (value == null) {
            return "(없음)";
        }

        return switch (field) {
            case "maxPowerCapacity", "currentPowerUsage", "measuredPower" -> value + " W";
            case "totalUnits", "usedUnits", "availableUnits" -> value + " U";
            case "gridX", "gridY" -> value.toString();
            case "status" -> translateStatus(value.toString());
            case "rackType" -> translateRackType(value.toString());
            case "doorDirection" -> translateDoorDirection(value.toString());
            case "zoneDirection" -> translateZoneDirection(value.toString());
            default -> value.toString();
        };
    }

    /**
     * 상태 번역
     */
    private String translateStatus(String status) {
        return switch (status) {
            case "ACTIVE" -> "활성";
            case "INACTIVE" -> "비활성";
            case "MAINTENANCE" -> "점검중";
            case "RESERVED" -> "예약됨";
            default -> status;
        };
    }

    /**
     * 랙 타입 번역
     */
    private String translateRackType(String rackType) {
        return switch (rackType) {
            case "STANDARD" -> "표준";
            case "NETWORK" -> "네트워크";
            case "SERVER" -> "서버";
            default -> rackType;
        };
    }

    /**
     * 도어 방향 번역
     */
    private String translateDoorDirection(String doorDirection) {
        return switch (doorDirection) {
            case "FRONT" -> "전면";
            case "REAR" -> "후면";
            case "BOTH" -> "양면";
            default -> doorDirection;
        };
    }

    /**
     * 존 방향 번역
     */
    private String translateZoneDirection(String zoneDirection) {
        return switch (zoneDirection) {
            case "NORTH" -> "북";
            case "SOUTH" -> "남";
            case "EAST" -> "동";
            case "WEST" -> "서";
            default -> zoneDirection;
        };
    }
}