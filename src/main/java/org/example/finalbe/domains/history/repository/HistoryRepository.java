/**
 * 작성자: 황요한
 * 히스토리 Repository
 */
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

@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    /**
     * 서버실별 히스토리 조회
     */
    Page<History> findByServerRoomIdOrderByChangedAtDesc(Long serverRoomId, Pageable pageable);

    /**
     * 서버실 + 기간별 히스토리 조회
     */
    Page<History> findByServerRoomIdAndChangedAtBetweenOrderByChangedAtDesc(
            Long serverRoomId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 특정 엔티티의 히스토리 조회
     */
    Page<History> findByServerRoomIdAndEntityTypeAndEntityIdOrderByChangedAtDesc(
            Long serverRoomId,
            EntityType entityType,
            Long entityId,
            Pageable pageable
    );

    /**
     * 엔티티 타입과 ID로 히스토리 조회
     */
    Page<History> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            EntityType entityType,
            Long entityId,
            Pageable pageable
    );

    /**
     * 서버실 + 엔티티 타입별 히스토리 조회
     */
    Page<History> findByServerRoomIdAndEntityTypeOrderByChangedAtDesc(
            Long serverRoomId,
            EntityType entityType,
            Pageable pageable
    );

    /**
     * 서버실 + 작업 타입별 히스토리 조회
     */
    Page<History> findByServerRoomIdAndActionOrderByChangedAtDesc(
            Long serverRoomId,
            HistoryAction action,
            Pageable pageable
    );

    /**
     * 복합 조건 검색
     */
    @Query("SELECT h FROM History h WHERE " +
            "(COALESCE(:serverRoomId, h.serverRoomId) = h.serverRoomId) AND " +
            "(COALESCE(:entityType, h.entityType) = h.entityType) AND " +
            "(COALESCE(:action, h.action) = h.action) AND " +
            "(COALESCE(:changedBy, h.changedBy) = h.changedBy) AND " +
            "(h.changedAt >= COALESCE(:startDate, CAST('1970-01-01 00:00:00' AS LocalDateTime))) AND " +
            "(h.changedAt <= COALESCE(:endDate, CAST('2099-12-31 23:59:59' AS LocalDateTime))) " +
            "ORDER BY h.changedAt DESC")
    Page<History> searchHistory(
            @Param("serverRoomId") Long serverRoomId,
            @Param("entityType") EntityType entityType,
            @Param("action") HistoryAction action,
            @Param("changedBy") Long changedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 서버실별 작업 타입 통계
     */
    @Query("SELECT h.action, COUNT(h) FROM History h " +
            "WHERE h.serverRoomId = :serverRoomId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY h.action")
    List<Object[]> countByActionAndDateRange(
            @Param("serverRoomId") Long serverRoomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 서버실별 엔티티 타입 통계
     */
    @Query("SELECT h.entityType, COUNT(h) FROM History h " +
            "WHERE h.serverRoomId = :serverRoomId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY h.entityType")
    List<Object[]> countByEntityTypeAndDateRange(
            @Param("serverRoomId") Long serverRoomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 최근 활동 많은 자산 TOP N
     */
    @Query("SELECT h.entityType, h.entityId, h.entityName, COUNT(h) as cnt FROM History h " +
            "WHERE h.serverRoomId = :serverRoomId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY h.entityType, h.entityId, h.entityName " +
            "ORDER BY cnt DESC")
    List<Object[]> findTopActiveEntities(
            @Param("serverRoomId") Long serverRoomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 최근 활동 많은 사용자 TOP N
     */
    @Query("SELECT h.changedBy, h.changedByName, COUNT(h) as cnt FROM History h " +
            "WHERE h.serverRoomId = :serverRoomId " +
            "AND h.changedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY h.changedBy, h.changedByName " +
            "ORDER BY cnt DESC")
    List<Object[]> findTopActiveUsers(
            @Param("serverRoomId") Long serverRoomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 사용자별 히스토리 조회
     */
    Page<History> findByChangedByOrderByChangedAtDesc(Long changedBy, Pageable pageable);


}