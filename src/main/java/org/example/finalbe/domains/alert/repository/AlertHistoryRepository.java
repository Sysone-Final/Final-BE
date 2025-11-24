package org.example.finalbe.domains.alert.repository;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    // ========== 기존 조회 메서드 (status 제거) ==========

    List<AlertHistory> findByEquipmentIdOrderByTriggeredAtDesc(Long equipmentId);

    @Query("SELECT a FROM AlertHistory a WHERE a.equipmentId = :equipmentId " +
            "AND a.metricType = :metricType AND a.metricName = :metricName " +
            "ORDER BY a.triggeredAt DESC")
    List<AlertHistory> findActiveAlertsByEquipmentIdAndMetric(
            @Param("equipmentId") Long equipmentId,
            @Param("metricType") MetricType metricType,
            @Param("metricName") String metricName);

    List<AlertHistory> findByRackIdOrderByTriggeredAtDesc(Long rackId);

    @Query("SELECT a FROM AlertHistory a WHERE a.rackId = :rackId " +
            "AND a.metricType = :metricType AND a.metricName = :metricName " +
            "ORDER BY a.triggeredAt DESC")
    List<AlertHistory> findActiveAlertsByRackIdAndMetric(
            @Param("rackId") Long rackId,
            @Param("metricType") MetricType metricType,
            @Param("metricName") String metricName);

    List<AlertHistory> findAllByOrderByTriggeredAtDesc();

    List<AlertHistory> findByServerRoomIdOrderByTriggeredAtDesc(Long serverRoomId);

    List<AlertHistory> findByDataCenterIdOrderByTriggeredAtDesc(Long dataCenterId);

    // ========== 페이지네이션 및 필터링 메서드 ==========

    @Query("SELECT a FROM AlertHistory a " +
            "WHERE a.serverRoomId IN :serverRoomIds " +
            "AND a.triggeredAt >= :startTime " +
            "AND a.targetType != :excludeTargetType")
    Page<AlertHistory> findByServerRoomIdInAndTriggeredAtAfterAndTargetTypeNot(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("excludeTargetType") TargetType excludeTargetType,
            Pageable pageable);

    @Query("SELECT a FROM AlertHistory a " +
            "WHERE a.serverRoomId IN :serverRoomIds " +
            "AND a.level = :level " +
            "AND a.triggeredAt >= :startTime " +
            "AND a.targetType != :excludeTargetType")
    Page<AlertHistory> findByServerRoomIdInAndLevelAndTriggeredAtAfterAndTargetTypeNot(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("level") AlertLevel level,
            @Param("startTime") LocalDateTime startTime,
            @Param("excludeTargetType") TargetType excludeTargetType,
            Pageable pageable);

    // ========== 통계 조회 메서드 (서버실별 필터링) ==========

    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds")
    long countByServerRoomIdIn(@Param("serverRoomIds") List<Long> serverRoomIds);

    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds AND a.level = :level")
    long countByServerRoomIdInAndLevel(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("level") AlertLevel level);

    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds AND a.targetType = :targetType")
    long countByServerRoomIdInAndTargetType(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("targetType") TargetType targetType);

    // ========== 기존 통계 조회 메서드 (전체 기준) ==========

    long countByLevel(AlertLevel level);

    long countByTargetType(TargetType targetType);

    // ========== 읽음 처리 관련 메서드 ==========

    @Modifying
    @Query("UPDATE AlertHistory a SET a.isRead = true, a.readAt = :readAt, a.readBy = :readBy " +
            "WHERE a.serverRoomId IN :serverRoomIds AND a.isRead = false")
    int markAllAsReadByServerRoomIds(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("readAt") LocalDateTime readAt,
            @Param("readBy") Long readBy
    );

    @Modifying
    @Query("UPDATE AlertHistory a SET a.isRead = true, a.readAt = :readAt, a.readBy = :readBy " +
            "WHERE a.id IN :alertIds AND a.isRead = false")
    int markAsReadByIds(
            @Param("alertIds") List<Long> alertIds,
            @Param("readAt") LocalDateTime readAt,
            @Param("readBy") Long readBy
    );

    // ========== 삭제 관련 메서드 ==========

    @Modifying
    @Query("DELETE FROM AlertHistory a WHERE a.id IN :alertIds AND a.serverRoomId IN :serverRoomIds")
    int deleteByIdsAndServerRoomIds(
            @Param("alertIds") List<Long> alertIds,
            @Param("serverRoomIds") List<Long> serverRoomIds
    );

    @Modifying
    @Query("DELETE FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds")
    int deleteAllByServerRoomIds(@Param("serverRoomIds") List<Long> serverRoomIds);

    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds AND a.isRead = false")
    long countUnreadByServerRoomIds(@Param("serverRoomIds") List<Long> serverRoomIds);
}