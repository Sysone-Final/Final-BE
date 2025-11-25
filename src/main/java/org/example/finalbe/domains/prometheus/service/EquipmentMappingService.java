package org.example.finalbe.domains.prometheus.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentMappingService {

    private final EquipmentRepository equipmentRepository;

    private final Map<String, Long> instanceToEquipmentIdMap = new HashMap<>();
    private final Map<Long, String> equipmentIdToInstanceMap = new HashMap<>();
    private final Map<Long, Equipment> equipmentCache = new HashMap<>();

    @PostConstruct
    @Transactional(readOnly = true)
    public void initialize() {
        log.info("🔄 ===============================================");
        log.info("🔄 Equipment 매핑 초기화 시작...");

        List<Equipment> allEquipments = equipmentRepository.findAll();
        log.info("🔄 DB 전체 장비: {} 개", allEquipments.size());

        // 256-259 특별 체크
        for (long id = 256; id <= 259; id++) {
            final long equipId = id;
            Optional<Equipment> eq = allEquipments.stream()
                    .filter(e -> e.getId().equals(equipId))
                    .findFirst();

            if (eq.isPresent()) {
                Equipment e = eq.get();
                log.info("  🔍 Equipment {}: code={}, rack={}, delYn={}",
                        e.getId(),
                        e.getCode(),
                        e.getRack() != null ? e.getRack().getId() : "NULL",
                        e.getDelYn());
            } else {
                log.warn("  ❌ Equipment {} DB에 없음!", equipId);
            }
        }

        List<Equipment> equipments = allEquipments.stream()
                .filter(e -> DelYN.N.equals(e.getDelYn()))
                .filter(e -> e.getRack() != null)
                .toList();

        log.info("🔄 매핑 대상: {} 개 (활성 + 랙배치)", equipments.size());

        int mappedCount = 0;
        for (Equipment equipment : equipments) {
            if (equipment.getCode() != null && !equipment.getCode().trim().isEmpty()) {
                String code = equipment.getCode().trim();
                instanceToEquipmentIdMap.put(code, equipment.getId());
                equipmentIdToInstanceMap.put(equipment.getId(), code);
                equipmentCache.put(equipment.getId(), equipment);

                // 256-259 특별 로그
                if (equipment.getId() >= 256 && equipment.getId() <= 259) {
                    log.info("  ✅✅✅ [중요] 매핑 성공: {} → Equipment {}",
                            code, equipment.getId());
                }
                mappedCount++;
            } else {
                if (equipment.getId() >= 256 && equipment.getId() <= 259) {
                    log.error("  ❌❌❌ [중요] Equipment {} code 없음!", equipment.getId());
                }
            }
        }

        log.info("🔄 최종 매핑: {} 개", mappedCount);
        log.info("🔄 256-259 매핑 확인:");
        for (long id = 256; id <= 259; id++) {
            String instance = equipmentIdToInstanceMap.get(id);
            log.info("  - Equipment {}: {}", id,
                    instance != null ? instance : "매핑 안 됨!");
        }
        log.info("🔄 ===============================================");
    }

    /**
     * ✅ 새로 추가: 장비를 매핑에 추가 (랙 배치 시 호출)
     */
    public void addEquipmentMapping(Equipment equipment) {
        if (equipment.getRack() == null) {
            log.warn("⚠️ 장비 ID: {} - 랙에 배치되지 않아 메트릭 수집 매핑을 추가하지 않습니다.",
                    equipment.getId());
            return;
        }

        if (equipment.getCode() != null && !equipment.getCode().trim().isEmpty()) {
            String code = equipment.getCode().trim();
            instanceToEquipmentIdMap.put(code, equipment.getId());
            equipmentIdToInstanceMap.put(equipment.getId(), code);
            equipmentCache.put(equipment.getId(), equipment);
            log.info("✅ 메트릭 수집 매핑 추가: {} → Equipment ID: {} (Rack: {})",
                    code, equipment.getId(), equipment.getRack().getId());
        } else {
            log.warn("⚠️ 장비 ID: {} - code가 없어 메트릭 수집 매핑을 추가할 수 없습니다.",
                    equipment.getId());
        }
    }

    /**
     * ✅ 새로 추가: 장비 매핑 제거 (랙에서 제거 시 호출)
     */
    public void removeEquipmentMapping(Long equipmentId) {
        Optional<String> instance = getInstance(equipmentId);
        instance.ifPresent(inst -> {
            instanceToEquipmentIdMap.remove(inst);
            equipmentIdToInstanceMap.remove(equipmentId);
            equipmentCache.remove(equipmentId);
            log.info("✅ 메트릭 수집 매핑 제거: Equipment ID: {}", equipmentId);
        });

        if (instance.isEmpty()) {
            log.debug("⊘ Equipment ID: {} - 매핑이 존재하지 않아 제거 작업 스킵", equipmentId);
        }
    }

    /**
     * ✅ 새로 추가: 장비 매핑 업데이트 (랙 변경 시 호출)
     */
    public void updateEquipmentMapping(Equipment equipment) {
        // 기존 매핑 제거 후 재추가
        removeEquipmentMapping(equipment.getId());

        if (equipment.getRack() != null) {
            addEquipmentMapping(equipment);
            log.info("✅ 메트릭 수집 매핑 업데이트: Equipment ID: {} → Rack: {}",
                    equipment.getId(), equipment.getRack().getId());
        } else {
            log.info("⊘ 메트릭 수집 중단: Equipment ID: {} - 랙에서 제거됨",
                    equipment.getId());
        }
    }

    public Optional<Long> getEquipmentId(String instance) {
        return Optional.ofNullable(instanceToEquipmentIdMap.get(instance));
    }

    public Optional<String> getInstance(Long equipmentId) {
        return Optional.ofNullable(equipmentIdToInstanceMap.get(equipmentId));
    }

    public Optional<Equipment> getEquipment(Long equipmentId) {
        return Optional.ofNullable(equipmentCache.get(equipmentId));
    }

    public List<String> getAllInstances() {
        return List.copyOf(instanceToEquipmentIdMap.keySet());
    }

    public void refresh() {
        instanceToEquipmentIdMap.clear();
        equipmentIdToInstanceMap.clear();
        equipmentCache.clear();
        initialize();
    }

    /**
     * ✅ 새로 추가: 현재 매핑 상태 확인 (디버깅용)
     */
    public Map<String, Object> getMappingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalMappings", instanceToEquipmentIdMap.size());
        status.put("mappedEquipmentIds", List.copyOf(equipmentIdToInstanceMap.keySet()));
        status.put("mappedInstances", List.copyOf(instanceToEquipmentIdMap.keySet()));
        return status;
    }
}