// 작성자: 황요한
// 설명: 장비(Equipment) 관련 데이터베이스 조회를 담당하는 Repository

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

    // ID로 활성 장비 조회
    @Query("SELECT e FROM Equipment e WHERE e.id = :id AND e.delYn = 'N'")
    Optional<Equipment> findActiveById(@Param("id") Long id);

    // 관리자용 장비 검색 (필터 + 페이지네이션)
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

    // 회사별 장비 검색 (필터 + 페이지네이션)
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

    // 랙 ID로 장비 조회
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "WHERE r.id = :rackId " +
            "AND e.delYn = :delYn " +
            "AND r.delYn = 'N'")
    List<Equipment> findByRackIdAndDelYn(
            @Param("rackId") Long rackId,
            @Param("delYn") DelYN delYn
    );

    // 특정 랙의 활성 장비 조회
    @Query("SELECT e FROM Equipment e " +
            "WHERE e.rack.id = :rackId " +
            "AND e.delYn = 'N'")
    List<Equipment> findActiveByRackId(@Param("rackId") Long rackId);

    // 서버실 ID로 장비 조회
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

    // 장비 코드 중복 체크
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    // 키워드 검색
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

    // 키워드 + 회사 조건 검색
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

    // 전체 활성 장비 조회
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN e.rack r " +
            "WHERE e.delYn = 'N' " +
            "AND (r IS NULL OR r.delYn = 'N')")
    List<Equipment> findAllActive();

    // ID로 조회 (Rack, ServerRoom까지 fetch)
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.serverRoom sr " +
            "WHERE e.id = :id")
    Optional<Equipment> findByIdWithRackAndServerRoom(@Param("id") Long id);

    List<Equipment> findByDelYn(DelYN delYN);

    // 여러 랙의 장비 목록 조회
    @Query("SELECT e FROM Equipment e WHERE e.rack.id IN :rackIds AND e.delYn = :delYn")
    List<Equipment> findByRackIdInAndDelYn(@Param("rackIds") List<Long> rackIds, @Param("delYn") DelYN delYn);

    // 여러 랙의 특정 상태 장비 개수 조회
    @Query("SELECT COUNT(e) FROM Equipment e WHERE e.rack.id IN :rackIds AND e.status = :status AND e.delYn = :delYn")
    long countByRackIdInAndStatusAndDelYn(
            @Param("rackIds") List<Long> rackIds,
            @Param("status") EquipmentStatus status,
            @Param("delYn") DelYN delYn
    );

    // 장비 전체 구조 Fetch Join
    @Query("SELECT e FROM Equipment e " +
            "LEFT JOIN FETCH e.rack r " +
            "LEFT JOIN FETCH r.serverRoom sr " +
            "LEFT JOIN FETCH sr.dataCenter dc " +
            "WHERE e.id = :equipmentId")
    Optional<Equipment> findByIdWithFullHierarchy(@Param("equipmentId") Long equipmentId);

    // 특정 서버실에서 장비가 배치된 랙 ID 목록 조회
    @Query("SELECT DISTINCT e.rack.id FROM Equipment e " +
            "WHERE e.rack.serverRoom.id = :serverRoomId " +
            "AND e.delYn = 'N' " +
            "AND e.rack.delYn = 'N' " +
            "AND e.rack IS NOT NULL")
    List<Long> findDistinctRackIdsByServerRoomId(@Param("serverRoomId") Long serverRoomId);

    // 전체 시스템의 랙 ID 목록 조회
    @Query("SELECT DISTINCT e.rack.id FROM Equipment e " +
            "WHERE e.delYn = 'N' " +
            "AND e.rack IS NOT NULL " +
            "AND e.rack.delYn = 'N'")
    List<Long> findAllDistinctRackIds();

    // 여러 랙의 장비 개수 조회 (Projection)
    @Query("SELECT e.rack.id as rackId, COUNT(e) as count " +
            "FROM Equipment e " +
            "WHERE e.rack.id IN :rackIds " +
            "AND e.delYn = :delYn " +
            "GROUP BY e.rack.id")
    List<RackEquipmentCount> countEquipmentsByRackIds(
            @Param("rackIds") List<Long> rackIds,
            @Param("delYn") DelYN delYn
    );

    interface RackEquipmentCount {
        Long getRackId();
        Long getCount();
    }
}
