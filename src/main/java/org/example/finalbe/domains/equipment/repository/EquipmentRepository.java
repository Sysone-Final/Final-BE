package org.example.finalbe.domains.equipment.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByRackIdAndDelYn(Long rackId, DelYN delYn);

    boolean existsByRackIdAndDelYn(Long rackId, DelYN delYn);
}