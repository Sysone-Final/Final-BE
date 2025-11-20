package org.example.finalbe.domains.alert.repository;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertStatus;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

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
}