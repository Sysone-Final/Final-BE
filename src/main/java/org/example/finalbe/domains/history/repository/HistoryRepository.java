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
 * 엔티티 변경 이력 조회 및 통계 제공
 */
@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

    // ========== 기본 조회 메서드 ==========

    /**
     * 서버실별 히스토리 조회 (페이징)
     * 예: 특정 서버실의 모든 변경 이력
     */
    Page<History> findByServerRoomIdOrderByChangedAtDesc(Long serverRoomId, Pageable pageable);

    /**
     * 서버실 + 기간별 히스토리 조회
     * 예: 특정 서버실의 이번 주 변경 이력
     */
    Page<History> findByServerRoomIdAndChangedAtBetweenOrderByChangedAtDesc(
            Long serverRoomId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 특정 엔티티의 히스토리 조회 (서버실 ID 포함)
     * 예: 특정 서버실의 특정 랙의 변경 이력
     */
    Page<History> findByServerRoomIdAndEntityTypeAndEntityIdOrderByChangedAtDesc(
            Long serverRoomId,
            EntityType entityType,
            Long entityId,
            Pageable pageable
    );

    /**
     * 엔티티 타입과 ID로 히스토리 조회 (서버실 ID 없이)
     * 예: 랙 ID=123의 모든 변경 이력
     * ⭐ 상세 조회 API에서 사용
     */
    Page<History> findByEntityTypeAndEntityIdOrderByChangedAtDesc(
            EntityType entityType,
            Long entityId,
            Pageable pageable
    );

    /**
     * 서버실 + 엔티티 타입별 히스토리 조회
     * 예: 특정 서버실의 모든 랙 변경 이력
     */
    Page<History> findByServerRoomIdAndEntityTypeOrderByChangedAtDesc(
            Long serverRoomId,
            EntityType entityType,
            Pageable pageable
    );

    /**
     * 서버실 + 작업 타입별 히스토리 조회
     * 예: 특정 서버실의 모든 삭제(DELETE) 이력
     */
    Page<History> findByServerRoomIdAndActionOrderByChangedAtDesc(
            Long serverRoomId,
            HistoryAction action,
            Pageable pageable
    );

    // ========== 복합 검색 ==========

    /**
     * 복합 조건 검색 (서버실 + 엔티티타입 + 작업타입 + 기간 + 사용자)
     * null 값은 조건에서 제외됨 (유연한 검색)
     *
     * 사용 예시:
     * - 서버실 1번의 RACK 엔티티 + UPDATE 액션만 조회
     * - 서버실 1번의 홍길동(userId=5)의 이번 주 작업 조회
     */
    @Query("SELECT h FROM History h WHERE " +
            "(:serverRoomId IS NULL OR h.serverRoomId = :serverRoomId) AND " +
            "(:entityType IS NULL OR h.entityType = :entityType) AND " +
            "(:action IS NULL OR h.action = :action) AND " +
            "(:changedBy IS NULL OR h.changedBy = :changedBy) AND " +
            "(:startDate IS NULL OR h.changedAt >= :startDate) AND " +
            "(:endDate IS NULL OR h.changedAt <= :endDate) " +
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

    // ========== 통계 쿼리 ==========

    /**
     * 서버실별 작업 타입 통계 (기간별)
     * 예: 이번 주 CREATE: 50건, UPDATE: 120건, DELETE: 10건
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
     * 서버실별 엔티티 타입 통계 (기간별)
     * 예: 이번 주 RACK: 30건, EQUIPMENT: 80건, DEVICE: 40건
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
     * 예: 가장 많이 변경된 랙/장비 10개 조회
     *
     * 반환: [EntityType, Long entityId, String entityName, Long changeCount]
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
     * 예: 가장 많이 작업한 사용자 10명 조회
     *
     * 반환: [Long userId, String userName, Long changeCount]
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

    // ========== 사용자별 조회 ==========

    /**
     * 사용자별 히스토리 조회
     * 예: 홍길동(userId=5)의 모든 작업 이력
     */
    Page<History> findByChangedByOrderByChangedAtDesc(Long changedBy, Pageable pageable);

    /**
     * 서버실 + 사용자별 히스토리 조회
     * 예: 서버실 1번에서 홍길동의 작업 이력
     */
    Page<History> findByServerRoomIdAndChangedByOrderByChangedAtDesc(
            Long serverRoomId,
            Long changedBy,
            Pageable pageable
    );
}