package org.example.finalbe.domains.history.repository;

import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;
import org.example.finalbe.domains.history.domain.History;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 히스토리 Repository
 */
@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    /**
     * 서버실별 히스토리 조회 (페이징)
     */
    Page<History> findByDataCenterIdOrderByChangedAtDesc(Long dataCenterId, Pageable pageable);

    /**
     * 서버실 + 기간별 히스토리 조회
     */
    Page<History> findByDataCenterIdAndChangedAtBetweenOrderByChangedAtDesc(
            Long dataCenterId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 특정 엔티티의 히스토리 조회
     */
    Page<History> findByDataCenterIdAndEntityTypeAndEntityIdOrderByChangedAtDesc(
            Long dataCenterId,
            EntityType entityType,
            Long entityId,
            Pageable pageable
    );

    /**
     * 서버실 + 엔티티 타입별 히스토리 조회
     */
    Page<History> findByDataCenterIdAndEntityTypeOrderByChangedAtDesc(
            Long dataCenterId,
            EntityType entityType,
            Pageable pageable
    );

    /**
     * 서버실 + 작업 타입별 히스토리 조회
     */
    Page<History> findByDataCenterIdAndActionOrderByChangedAtDesc(
            Long dataCenterId,
            HistoryAction action,
            Pageable pageable
    );

    /**
     * 사용자별 히스토리 조회
     */
    Page<History> findByChangedByOrderByChangedAtDesc(Long changedBy, Pageable pageable);

    /**
     * 서버실 + 사용자별 히스토리 조회
     */
    Page<History> findByDataCenterIdAndChangedByOrderByChangedAtDesc(
            Long dataCenterId,
            Long changedBy,
            Pageable pageable
    );

    /**
     * 복합 조건 검색 (서버실 + 엔티티타입 + 작업타입 + 기간 + 사용자)
     */
    @Query("SELECT h FROM History h WHERE " +
            "(:dataCenterId IS NULL OR h.dataCenterId = :dataCenterId) AND " +
            "(:entityType IS NULL OR h.entityType = :entityType) AND " +
            "(:action IS NULL OR h.action = :action) AND " +
            "(:changedBy IS NULL OR h.changedBy = :changedBy) AND " +
            "(:startDate IS NULL OR h.changedAt >= :startDate) AND " +
            "(:endDate IS NULL OR h.changedAt <= :endDate) " +
            "ORDER BY h.changedAt DESC")
    Page<History> searchHistory(
            @Param("dataCenterId") Long dataCenterId,
            @Param("entityType") EntityType entityType,
            @Param("action") HistoryAction action,
            @Param("changedBy") Long changedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 서버실별 통계 - 기간별 작업 수
     */
    @Query("SELECT h.action, COUNT(h) FROM History h " +
            "WHERE h.dataCenterId = :dataCenterId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY h.action")
    List<Object[]> countByActionAndDateRange(
            @Param("dataCenterId") Long dataCenterId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 서버실별 통계 - 엔티티별 작업 수
     */
    @Query("SELECT h.entityType, COUNT(h) FROM History h " +
            "WHERE h.dataCenterId = :dataCenterId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY h.entityType")
    List<Object[]> countByEntityTypeAndDateRange(
            @Param("dataCenterId") Long dataCenterId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 서버실별 통계 - 최근 활동 많은 자산 TOP N
     */
    @Query("SELECT h.entityType, h.entityId, h.entityName, COUNT(h) as cnt FROM History h " +
            "WHERE h.dataCenterId = :dataCenterId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY h.entityType, h.entityId, h.entityName " +
            "ORDER BY cnt DESC")
    List<Object[]> findTopActiveEntities(
            @Param("dataCenterId") Long dataCenterId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 서버실별 통계 - 최근 활동 많은 사용자 TOP N
     */
    @Query("SELECT h.changedBy, h.changedByName, COUNT(h) as cnt FROM History h " +
            "WHERE h.dataCenterId = :dataCenterId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY h.changedBy, h.changedByName " +
            "ORDER BY cnt DESC")
    List<Object[]> findTopActiveUsers(
            @Param("dataCenterId") Long dataCenterId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 특정 기간 이전 히스토리 삭제 (아카이빙용)
     */
    @Query("DELETE FROM History h WHERE h.changedAt < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}