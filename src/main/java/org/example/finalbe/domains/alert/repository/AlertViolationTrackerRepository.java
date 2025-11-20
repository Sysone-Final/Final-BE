package org.example.finalbe.domains.alert.repository;

import org.example.finalbe.domains.alert.domain.AlertViolationTracker;
import org.example.finalbe.domains.common.enumdir.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AlertViolationTrackerRepository extends JpaRepository<AlertViolationTracker, Long> {

    @Query("SELECT t FROM AlertViolationTracker t WHERE t.targetType = 'EQUIPMENT' " +
            "AND t.equipmentId = :equipmentId AND t.metricType = :metricType " +
            "AND t.metricName = :metricName")
    Optional<AlertViolationTracker> findByEquipmentIdAndMetric(
            @Param("equipmentId") Long equipmentId,
            @Param("metricType") MetricType metricType,
            @Param("metricName") String metricName);

    @Query("SELECT t FROM AlertViolationTracker t WHERE t.targetType = 'RACK' " +
            "AND t.rackId = :rackId AND t.metricType = :metricType " +
            "AND t.metricName = :metricName")
    Optional<AlertViolationTracker> findByRackIdAndMetric(
            @Param("rackId") Long rackId,
            @Param("metricType") MetricType metricType,
            @Param("metricName") String metricName);

    @Query("SELECT t FROM AlertViolationTracker t WHERE t.targetType = 'SERVER_ROOM' " +
            "AND t.serverRoomId = :serverRoomId AND t.metricType = :metricType " +
            "AND t.metricName = :metricName")
    Optional<AlertViolationTracker> findByServerRoomIdAndMetric(
            @Param("serverRoomId") Long serverRoomId,
            @Param("metricType") MetricType metricType,
            @Param("metricName") String metricName);

    @Query("SELECT t FROM AlertViolationTracker t WHERE t.targetType = 'DATA_CENTER' " +
            "AND t.dataCenterId = :dataCenterId AND t.metricType = :metricType " +
            "AND t.metricName = :metricName")
    Optional<AlertViolationTracker> findByDataCenterIdAndMetric(
            @Param("dataCenterId") Long dataCenterId,
            @Param("metricType") MetricType metricType,
            @Param("metricName") String metricName);
}