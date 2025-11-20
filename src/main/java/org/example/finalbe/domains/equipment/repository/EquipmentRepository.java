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
     * ID로 활성 장비 조회
     */
    @Query("SELECT e FROM Equipment e WHERE e.id = :id AND e.delYn = 'N'")
    Optional<Equipment> findActiveById(@Param("id") Long id);

    /**
     * 전체 장비 검색 (ADMIN용 - 페이지네이션 + 필터)
     */
    @Query("""
        SELECT e FROM Equipment e
        LEFT JOIN e.rack r
        LEFT JOIN r.serverRoom sr
        WHERE (:keyword IS NULL OR 
               LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:type IS NULL OR e.type = :type)
        AND (:status IS NULL OR e.status = :status)
        AND (:serverRoomId IS NULL OR sr.id = :serverRoomId)
        AND (:onlyUnassigned IS NULL OR :onlyUnassigned = FALSE OR e.rack IS NULL)
        AND e.delYn = :delYn
        AND (r IS NULL OR r.delYn = 'N')
        AND (sr IS NULL OR sr.delYn = 'N')
        """)
    Page<Equipment> searchEquipmentsWithFilters(
            @Param("keyword") String keyword,
            @Param("type") EquipmentType type,
            @Param("status") EquipmentStatus status,
            @Param("serverRoomId") Long serverRoomId,
            @Param("onlyUnassigned") Boolean onlyUnassigned,
            @Param("delYn") DelYN delYn,
            Pageable pageable
    );

    /**
     * 회사별 장비 검색 (일반 사용자용 - 페이지네이션 + 필터)
     */
    @Query("""
        SELECT e FROM Equipment e
        LEFT JOIN e.rack r
        LEFT JOIN r.serverRoom sr
        WHERE e.companyId = :companyId
        AND (:keyword IS NULL OR 
             LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:type IS NULL OR e.type = :type)
        AND (:status IS NULL OR e.status = :status)
        AND (:serverRoomId IS NULL OR sr.id = :serverRoomId)
        AND (:onlyUnassigned IS NULL OR :onlyUnassigned = FALSE OR e.rack IS NULL)
        AND e.delYn = :delYn
        AND (r IS NULL OR r.delYn = 'N')
        AND (sr IS NULL OR sr.delYn = 'N')
        """)
    Page<Equipment> searchEquipmentsWithFiltersByCompany(
            @Param("keyword") String keyword,
            @Param("type") EquipmentType type,
            @Param("status") EquipmentStatus status,
            @Param("serverRoomId") Long serverRoomId,
            @Param("onlyUnassigned") Boolean onlyUnassigned,
            @Param("companyId") Long companyId,
            @Param("delYn") DelYN delYn,
            Pageable pageable
    );

    /**
     * 랙별 장비 조회
     */
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "WHERE r.id = :rackId " +
            "AND e.delYn = :delYn " +
            "AND r.delYn = 'N'")
    List<Equipment> findByRackIdAndDelYn(
            @Param("rackId") Long rackId,
            @Param("delYn") DelYN delYn
    );

    /**
     * 특정 Rack의 활성 장비 조회 (랙 삭제 시 사용)
     */
    @Query("SELECT e FROM Equipment e " +
            "WHERE e.rack.id = :rackId " +
            "AND e.delYn = 'N'")
    List<Equipment> findActiveByRackId(@Param("rackId") Long rackId);

    /**
     * 서버실별 조회
     */
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.serverRoom sr " +
            "WHERE sr.id = :serverRoomId " +
            "AND e.delYn = :delYn " +
            "AND (r IS NULL OR r.delYn = 'N')")
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
            "AND e.delYn = :delYn " +
            "AND (r IS NULL OR r.delYn = 'N')")
    List<Equipment> searchByKeywordAndDelYn(
            @Param("keyword") String keyword,
            @Param("delYn") DelYN delYn
    );

    /**
     * 검색 (키워드 + 회사)
     */
    @Query("SELECT DISTINCT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.serverRoom sr " +
            "LEFT JOIN CompanyServerRoom csr ON csr.serverRoom.id = sr.id " +
            "WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND csr.company.id = :companyId " +
            "AND e.delYn = :delYn " +
            "AND (r IS NULL OR r.delYn = 'N')")
    List<Equipment> searchByKeywordAndCompanyIdAndDelYn(
            @Param("keyword") String keyword,
            @Param("companyId") Long companyId,
            @Param("delYn") DelYN delYn
    );

    /**
     * 활성 장비 전체 조회 (delYn = 'N', Rack도 활성)
     */
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN e.rack r " +
            "WHERE e.delYn = 'N' " +
            "AND (r IS NULL OR r.delYn = 'N')")
    List<Equipment> findAllActive();

    /**
     * ID로 장비 조회 (Rack, ServerRoom까지 fetch join)
     */
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.serverRoom sr " +
            "WHERE e.id = :id")
    Optional<Equipment> findByIdWithRackAndServerRoom(@Param("id") Long id);

    List<Equipment> findByDelYn(DelYN delYN);
}