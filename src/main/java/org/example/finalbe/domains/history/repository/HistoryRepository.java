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

    /**
     * 복합 조건 검색 - COALESCE 패턴 사용
     * PostgreSQL의 null 파라미터 타입 추론 문제 해결
     *
     * IS NULL OR 패턴 대신 COALESCE를 사용하여 항상 비교 연산 수행
     * - null인 경우: 모든 값과 매칭되도록 극단값 사용
     * - null이 아닌 경우: 실제 값으로 필터링
     *
     * @param serverRoomId 서버실 ID (null이면 모든 서버실)
     * @param entityType 엔티티 타입 (null이면 모든 타입)
     * @param action 작업 타입 (null이면 모든 작업)
     * @param changedBy 변경자 ID (null이면 모든 사용자)
     * @param startDate 시작 날짜 (null이면 과거 전체)
     * @param endDate 종료 날짜 (null이면 미래 전체)
     * @param pageable 페이징 정보
     * @return 조회된 히스토리 페이지
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