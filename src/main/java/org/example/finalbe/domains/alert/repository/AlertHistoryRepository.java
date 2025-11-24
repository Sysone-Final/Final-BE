package org.example.finalbe.domains.alert.repository;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
import org.example.finalbe.domains.common.enumdir.AlertStatus;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.example.finalbe.domains.common.enumdir.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // ========== 추가된 메서드 (ServerRoom 알림 조회) ==========

    /**
     * ServerRoom ID와 상태로 알림 조회
     */
    List<AlertHistory> findByServerRoomIdAndStatusOrderByTriggeredAtDesc(
            Long serverRoomId, AlertStatus status);

    /**
     * DataCenter ID와 상태로 알림 조회 (하위 호환성 유지)
     */
    List<AlertHistory> findByDataCenterIdAndStatusOrderByTriggeredAtDesc(
            Long dataCenterId, AlertStatus status);

    // ========== 통계 조회 메서드 ==========

    /**
     * 상태별 알림 개수 조회
     */
    long countByStatus(AlertStatus status);

    /**
     * 레벨별 알림 개수 조회
     */
    long countByLevel(AlertLevel level);

    /**
     * 타겟 타입별 알림 개수 조회
     */
    long countByTargetType(TargetType targetType);
}