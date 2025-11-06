package org.example.finalbe.domains.equipment.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {


    /**
     * ID로 활성 장비 조회 (delYn = N)
     */
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "WHERE e.id = :id AND e.delYn = 'N'")
    Optional<Equipment> findActiveById(@Param("id") Long id);
    // ========== 페이지네이션 조회 (전체 필터) ==========
    @Query(value = "SELECT DISTINCT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.datacenter dc " +
            "WHERE e.delYn = :delYn " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "    LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:type IS NULL OR :type = '' OR e.type = :type) " +
            "AND (:status IS NULL OR :status = '' OR e.status = :status) " +
            "AND (:datacenterId IS NULL OR dc.id = :datacenterId)",
            countQuery = "SELECT COUNT(DISTINCT e) FROM Equipment e " +
                    "LEFT JOIN e.rack r " +
                    "LEFT JOIN r.datacenter dc " +
                    "WHERE e.delYn = :delYn " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "    LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "    LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "    LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "AND (:type IS NULL OR :type = '' OR e.type = :type) " +
                    "AND (:status IS NULL OR :status = '' OR e.status = :status) " +
                    "AND (:datacenterId IS NULL OR dc.id = :datacenterId)")
    Page<Equipment> searchEquipmentsWithFilters(
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("status") String status,
            @Param("datacenterId") Long datacenterId,
            @Param("delYn") DelYN delYn,
            Pageable pageable
    );

    // ========== 기존 메서드들 ==========
    List<Equipment> findByRackIdAndDelYn(Long rackId, DelYN delYn);

    boolean existsByRackIdAndDelYn(Long rackId, DelYN delYn);

    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    // ========== 전산실별 조회 ==========
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.datacenter dc " +
            "WHERE dc.id = :datacenterId AND e.delYn = :delYn")
    List<Equipment> findByDatacenterIdAndDelYn(
            @Param("datacenterId") Long datacenterId,
            @Param("delYn") DelYN delYn
    );

    // ========== 검색 (키워드) ==========
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND e.delYn = :delYn")
    List<Equipment> searchByKeywordAndDelYn(
            @Param("keyword") String keyword,
            @Param("delYn") DelYN delYn
    );

    // ========== 검색 (키워드 + 회사) - 수정됨 ==========
    @Query("SELECT DISTINCT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.datacenter dc " +
            "LEFT JOIN CompanyDataCenter cdc ON cdc.dataCenter.id = dc.id " +
            "WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND cdc.company.id = :companyId " +
            "AND e.delYn = :delYn")
    List<Equipment> searchByKeywordAndCompanyIdAndDelYn(
            @Param("keyword") String keyword,
            @Param("companyId") Long companyId,
            @Param("delYn") DelYN delYn
    );


}