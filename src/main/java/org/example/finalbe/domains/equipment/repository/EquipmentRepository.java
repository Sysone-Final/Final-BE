package org.example.finalbe.domains.equipment.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Equipment 데이터 접근 계층
 */
@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    /**
     * 랙별 활성 장비 조회
     */
    List<Equipment> findByRackIdAndDelYn(Long rackId, DelYN delYn);

    /**
     * 랙별 활성 장비 존재 여부 확인
     */
    boolean existsByRackIdAndDelYn(Long rackId, DelYN delYn);

    /**
     * 장비 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * 전산실별 활성 장비 조회
     */
    @Query("SELECT e FROM Equipment e " +
            "JOIN e.rack r " +
            "WHERE r.datacenter.id = :datacenterId " +
            "AND e.delYn = :delYn")
    List<Equipment> findByDatacenterIdAndDelYn(
            @Param("datacenterId") Long datacenterId,
            @Param("delYn") DelYN delYn);

    /**
     * 키워드로 장비 검색
     */
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

    /**
     * 회사별 키워드로 장비 검색
     */
    @Query("SELECT e FROM Equipment e " +
            "JOIN e.rack r " +
            "JOIN CompanyDataCenter cdc ON r.datacenter.id = cdc.dataCenter.id " +
            "WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND cdc.company.id = :companyId " +
            "AND e.delYn = :delYn " +
            "AND cdc.delYn = 'N'")
    List<Equipment> searchByKeywordAndCompanyIdAndDelYn(
            @Param("keyword") String keyword,
            @Param("companyId") Long companyId,
            @Param("delYn") DelYN delYn);

    /**
     * 활성 장비 조회 (ID)
     */
    @Query("SELECT e FROM Equipment e WHERE e.id = :id AND e.delYn = 'N'")
    Optional<Equipment> findActiveById(@Param("id") Long id);

    /**
     *  모든 장비에 대한 페이지네이션 조회 (검색 포함)
     * JpaSpecificationExecutor를 사용하는 것이 더 좋지만, 우선은 Query로 구현합니다.
     */
    @Query(value = "SELECT e FROM Equipment e " +
            "WHERE e.delYn = 'N' " +
            "AND (:companyId IS NULL OR e.rack.datacenter.id IN (SELECT cdc.dataCenter.id FROM CompanyDataCenter cdc WHERE cdc.company.id = :companyId AND cdc.delYn = 'N')) " +
            "AND (:status IS NULL OR e.status = :status) " +
            "AND (:keyword IS NULL OR " +
            "   LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "   LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))",
            countQuery = "SELECT count(e) FROM Equipment e " +
                    "WHERE e.delYn = 'N' " +
                    "AND (:companyId IS NULL OR e.rack.datacenter.id IN (SELECT cdc.dataCenter.id FROM CompanyDataCenter cdc WHERE cdc.company.id = :companyId AND cdc.delYn = 'N')) " +
                    "AND (:status IS NULL OR e.status = :status) " +
                    "AND (:keyword IS NULL OR " +
                    "   LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "   LOWER(e.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "   LOWER(e.modelName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "   LOWER(e.ipAddress) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Equipment> findPaginatedEquipments(
            @Param("keyword") String keyword,
            @Param("status") String status, // Enum 대신 String으로 받거나, Service에서 변환
            @Param("companyId") Long companyId, // ADMIN이 아니면 회사 ID
            Pageable pageable
    );
}