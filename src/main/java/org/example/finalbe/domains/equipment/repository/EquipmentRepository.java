package org.example.finalbe.domains.equipment.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByRackIdAndDelYn(Long rackId, DelYN delYn);

    boolean existsByRackIdAndDelYn(Long rackId, DelYN delYn);

    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    @Query("SELECT e FROM Equipment e " +
            "JOIN e.rack r " +
            "WHERE r.datacenter.id = :datacenterId " +
            "AND e.delYn = :delYn")
    List<Equipment> findByDatacenterIdAndDelYn(
            @Param("datacenterId") Long datacenterId,
            @Param("delYn") DelYN delYn);

    @Query("SELECT e FROM Equipment e " +
            "WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND e.delYn = :delYn")
    List<Equipment> searchByKeywordAndDelYn(
            @Param("keyword") String keyword,
            @Param("delYn") DelYN delYn);

    @Query("SELECT e FROM Equipment e " +
            "JOIN e.rack r " +
            "WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND r.datacenter.company.id = :companyId " +
            "AND e.delYn = :delYn")
    List<Equipment> searchByKeywordAndCompanyIdAndDelYn(
            @Param("keyword") String keyword,
            @Param("companyId") Long companyId,
            @Param("delYn") DelYN delYn);

    @Query("SELECT e FROM Equipment e WHERE e.id = :id AND e.delYn = 'N'")
    Optional<Equipment> findActiveById(@Param("id") Long id);

    List<Equipment> findByManagerIdAndDelYn(Long managerId, DelYN delYn);

    @Query("SELECT COUNT(e) FROM Equipment e " +
            "WHERE e.status = :status " +
            "AND e.delYn = :delYn")
    Long countByStatusAndDelYn(
            @Param("status") EquipmentStatus status,
            @Param("delYn") DelYN delYn);

    @Query("SELECT COALESCE(SUM(e.powerConsumption), 0) FROM Equipment e " +
            "WHERE e.rack.id = :rackId " +
            "AND e.delYn = :delYn")
    Double sumPowerConsumptionByRackIdAndDelYn(
            @Param("rackId") Long rackId,
            @Param("delYn") DelYN delYn);

    @Query("SELECT COALESCE(SUM(e.weight), 0) FROM Equipment e " +
            "WHERE e.rack.id = :rackId " +
            "AND e.delYn = :delYn")
    Double sumWeightByRackIdAndDelYn(
            @Param("rackId") Long rackId,
            @Param("delYn") DelYN delYn);
}