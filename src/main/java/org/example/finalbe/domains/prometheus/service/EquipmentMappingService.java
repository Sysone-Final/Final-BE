package org.example.finalbe.domains.prometheus.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.info("🔄 Equipment 매핑 초기화 시작...");

        List<Equipment> equipments = equipmentRepository.findAll();
        int mappedCount = 0;

        for (Equipment equipment : equipments) {
            if (equipment.getCode() != null && !equipment.getCode().trim().isEmpty()) {
                String code = equipment.getCode().trim();
                instanceToEquipmentIdMap.put(code, equipment.getId());
                equipmentIdToInstanceMap.put(equipment.getId(), code);
                equipmentCache.put(equipment.getId(), equipment);
                mappedCount++;
                log.debug("  ✓ {} → Equipment ID: {}", code, equipment.getId());
            }
        }

        log.info("✅ Equipment 매핑 완료: {} / {} 개 장비 등록", mappedCount, equipments.size());
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
}