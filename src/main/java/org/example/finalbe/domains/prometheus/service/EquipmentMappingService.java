/**
 * 작성자: 황요한
 * 장비 코드(instance)와 장비 ID를 매핑하여 Prometheus 메트릭 수집에 사용되는 서비스
 */
package org.example.finalbe.domains.prometheus.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipmentMappingService {

    private final EquipmentRepository equipmentRepository;

    private final Map<String, Long> instanceToEquipmentIdMap = new HashMap<>();
    private final Map<Long, String> equipmentIdToInstanceMap = new HashMap<>();
    private final Map<Long, Equipment> equipmentCache = new HashMap<>();

    // DB에서 장비 목록을 읽어 매핑 초기화
    @PostConstruct
    @Transactional(readOnly = true)
    public void initialize() {
        List<Equipment> allEquipments = equipmentRepository.findAll();

        List<Equipment> equipments = allEquipments.stream()
                .filter(e -> DelYN.N.equals(e.getDelYn()))
                .filter(e -> e.getRack() != null)
                .toList();

        for (Equipment equipment : equipments) {
            if (equipment.getCode() != null && !equipment.getCode().trim().isEmpty()) {
                String code = equipment.getCode().trim();
                instanceToEquipmentIdMap.put(code, equipment.getId());
                equipmentIdToInstanceMap.put(equipment.getId(), code);
                equipmentCache.put(equipment.getId(), equipment);
            }
        }
    }

    // 장비를 매핑에 추가
    public void addEquipmentMapping(Equipment equipment) {
        if (equipment.getRack() == null) return;

        if (equipment.getCode() != null && !equipment.getCode().trim().isEmpty()) {
            String code = equipment.getCode().trim();
            instanceToEquipmentIdMap.put(code, equipment.getId());
            equipmentIdToInstanceMap.put(equipment.getId(), code);
            equipmentCache.put(equipment.getId(), equipment);
        }
    }

    // 장비 매핑 제거
    public void removeEquipmentMapping(Long equipmentId) {
        Optional<String> instance = getInstance(equipmentId);
        instance.ifPresent(inst -> {
            instanceToEquipmentIdMap.remove(inst);
            equipmentIdToInstanceMap.remove(equipmentId);
            equipmentCache.remove(equipmentId);
        });
    }

    // 장비 매핑 업데이트
    public void updateEquipmentMapping(Equipment equipment) {
        removeEquipmentMapping(equipment.getId());
        if (equipment.getRack() != null) {
            addEquipmentMapping(equipment);
        }
    }

    // instance로 equipmentId 조회
    public Optional<Long> getEquipmentId(String instance) {
        return Optional.ofNullable(instanceToEquipmentIdMap.get(instance));
    }

    // equipmentId로 instance 조회
    public Optional<String> getInstance(Long equipmentId) {
        return Optional.ofNullable(equipmentIdToInstanceMap.get(equipmentId));
    }

    // equipmentId로 Equipment 조회
    public Optional<Equipment> getEquipment(Long equipmentId) {
        return Optional.ofNullable(equipmentCache.get(equipmentId));
    }

    // 전체 instance 목록 조회
    public List<String> getAllInstances() {
        return List.copyOf(instanceToEquipmentIdMap.keySet());
    }

    // 매핑 전체 재초기화
    public void refresh() {
        instanceToEquipmentIdMap.clear();
        equipmentIdToInstanceMap.clear();
        equipmentCache.clear();
        initialize();
    }

    // 매핑 상태 반환
    public Map<String, Object> getMappingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalMappings", instanceToEquipmentIdMap.size());
        status.put("mappedEquipmentIds", List.copyOf(equipmentIdToInstanceMap.keySet()));
        status.put("mappedInstances", List.copyOf(instanceToEquipmentIdMap.keySet()));
        return status;
    }
}
