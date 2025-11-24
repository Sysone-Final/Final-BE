package org.example.finalbe.domains.alert.repository;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.AlertStatus;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    // ========== 기존 메서드 ==========

    List<AlertHistory> findByEquipmentIdAndStatusOrderByTriggeredAtDesc(
            Long equipmentId, AlertStatus status);

    @Query("SELECT a FROM AlertHistory a WHERE a.equipmentId = :equipmentId " +
            "AND a.metricType = :metricType AND a.metricName = :metricName " +
            "AND a.status != 'RESOLVED' ORDER BY a.triggeredAt DESC")
    List<AlertHistory> findActiveAlertsByEquipmentIdAndMetric(
            @Param("equipmentId") Long equipmentId,
            @Param("metricType") MetricType metricType,
            @Param("metricName") String metricName);

    List<AlertHistory> findByRackIdAndStatusOrderByTriggeredAtDesc(
            Long rackId, AlertStatus status);

    @Query("SELECT a FROM AlertHistory a WHERE a.rackId = :rackId " +
            "AND a.metricType = :metricType AND a.metricName = :metricName " +
            "AND a.status != 'RESOLVED' ORDER BY a.triggeredAt DESC")
    List<AlertHistory> findActiveAlertsByRackIdAndMetric(
            @Param("rackId") Long rackId,
            @Param("metricType") MetricType metricType,
            @Param("metricName") String metricName);

    List<AlertHistory> findByStatusOrderByTriggeredAtDesc(AlertStatus status);

    List<AlertHistory> findByServerRoomIdAndStatusOrderByTriggeredAtDesc(
            Long serverRoomId, AlertStatus status);

    List<AlertHistory> findByDataCenterIdAndStatusOrderByTriggeredAtDesc(
            Long dataCenterId, AlertStatus status);

    // ========== 페이지네이션 및 필터링 메서드 ==========

    /**
     * 서버실 ID 목록으로 알림 조회 (페이지네이션)
     * 상태, 시간 범위, 타겟 타입으로 필터링
     */
    @Query("SELECT a FROM AlertHistory a " +
            "WHERE a.serverRoomId IN :serverRoomIds " +
            "AND a.status = :status " +
            "AND a.triggeredAt >= :startTime " +
            "AND a.targetType != :excludeTargetType")
    Page<AlertHistory> findByServerRoomIdInAndStatusAndTriggeredAtAfterAndTargetTypeNot(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("status") AlertStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("excludeTargetType") TargetType excludeTargetType,
            Pageable pageable);

    /**
     * 서버실 ID 목록으로 알림 조회 (페이지네이션)
     * 상태, 레벨, 시간 범위, 타겟 타입으로 필터링
     */
    @Query("SELECT a FROM AlertHistory a " +
            "WHERE a.serverRoomId IN :serverRoomIds " +
            "AND a.status = :status " +
            "AND a.level = :level " +
            "AND a.triggeredAt >= :startTime " +
            "AND a.targetType != :excludeTargetType")
    Page<AlertHistory> findByServerRoomIdInAndStatusAndLevelAndTriggeredAtAfterAndTargetTypeNot(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("status") AlertStatus status,
            @Param("level") AlertLevel level,
            @Param("startTime") LocalDateTime startTime,
            @Param("excludeTargetType") TargetType excludeTargetType,
            Pageable pageable);

    // ========== 통계 조회 메서드 (서버실별 필터링) ==========

    /**
     * 서버실 ID 목록으로 전체 알림 개수 조회
     */
    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds")
    long countByServerRoomIdIn(@Param("serverRoomIds") List<Long> serverRoomIds);

    /**
     * 서버실 ID 목록과 상태로 알림 개수 조회
     */
    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds AND a.status = :status")
    long countByServerRoomIdInAndStatus(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("status") AlertStatus status);

    /**
     * 서버실 ID 목록과 레벨로 알림 개수 조회
     */
    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds AND a.level = :level")
    long countByServerRoomIdInAndLevel(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("level") AlertLevel level);

    /**
     * 서버실 ID 목록과 타겟 타입으로 알림 개수 조회
     */
    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds AND a.targetType = :targetType")
    long countByServerRoomIdInAndTargetType(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("targetType") TargetType targetType);

    // ========== 기존 통계 조회 메서드 (전체 기준) ==========

    long countByStatus(AlertStatus status);

    long countByLevel(AlertLevel level);

    long countByTargetType(TargetType targetType);
}