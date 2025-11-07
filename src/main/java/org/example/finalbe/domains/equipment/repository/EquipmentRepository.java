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

    /**
     * 페이지네이션 조회 (전체 필터 + 서버실 필터 포함)
     */
    @Query(value = "SELECT DISTINCT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.serverroom sr " +
            "WHERE e.delYn = :delYn " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "    LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:type IS NULL OR e.type = :type) " +
            "AND (:status IS NULL OR e.status = :status) " +
            "AND (:serverRoomId IS NULL OR sr.id = :serverRoomId)",
            countQuery = "SELECT COUNT(DISTINCT e) FROM Equipment e " +
                    "LEFT JOIN e.rack r " +
                    "LEFT JOIN r.serverroom sr " +
                    "WHERE e.delYn = :delYn " +
                    "AND (:keyword IS NULL OR :keyword = '' OR " +
                    "    LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "    LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "    LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                    "AND (:type IS NULL OR e.type = :type) " +
                    "AND (:status IS NULL OR e.status = :status) " +
                    "AND (:serverRoomId IS NULL OR sr.id = :serverRoomId)")
    Page<Equipment> searchEquipmentsWithFilters(
            @Param("keyword") String keyword,
            @Param("type") EquipmentType type,
            @Param("status") EquipmentStatus status,
            @Param("serverRoomId") Long serverRoomId,
            @Param("delYn") DelYN delYn,
            Pageable pageable
    );

    /**
     * 랙별 장비 조회
     */
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "WHERE r.id = :rackId AND e.delYn = :delYn")
    List<Equipment> findByRackIdAndDelYn(
            @Param("rackId") Long rackId,
            @Param("delYn") DelYN delYn
    );

    /**
     * 서버실별 조회
     */
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.serverroom sr " +
            "WHERE sr.id = :serverRoomId AND e.delYn = :delYn")
    List<Equipment> findByServerRoomIdAndDelYn(
            @Param("serverRoomId") Long serverRoomId,
            @Param("delYn") DelYN delYn
    );

    /**
     * 장비 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * 검색 (키워드)
     */
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

    /**
     * 검색 (키워드 + 회사)
     */
    @Query("SELECT DISTINCT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.serverroom sr " +
            "LEFT JOIN CompanyServerRoom csr ON csr.serverRoom.id = sr.id " +
            "WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND csr.company.id = :companyId " +
            "AND e.delYn = :delYn")
    List<Equipment> searchByKeywordAndCompanyIdAndDelYn(
            @Param("keyword") String keyword,
            @Param("companyId") Long companyId,
            @Param("delYn") DelYN delYn
    );

    Boolean existsByRackIdAndDelYn(Long rackId, DelYN delYn);
}