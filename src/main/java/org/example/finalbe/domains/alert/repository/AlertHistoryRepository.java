/**
 * 작성자: 황요한
 * 알림 이력(AlertHistory) 조회, 통계, 읽음 처리, 삭제 기능을 제공하는 Repository
 */
package org.example.finalbe.domains.alert.repository;

import org.example.finalbe.domains.alert.domain.AlertHistory;
import org.example.finalbe.domains.common.enumdir.AlertLevel;
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

    // 개별 단위 조회
    List<AlertHistory> findByEquipmentIdOrderByTriggeredAtDesc(Long equipmentId);
    List<AlertHistory> findByRackIdOrderByTriggeredAtDesc(Long rackId);
    List<AlertHistory> findByServerRoomIdOrderByTriggeredAtDesc(Long serverRoomId);

    // 서버실 범위 + 기간 필터 조회
    @Query("""
            SELECT a FROM AlertHistory a
            WHERE a.serverRoomId IN :serverRoomIds
              AND a.triggeredAt >= :startTime
              AND a.targetType <> :excludeTargetType
            """)
    Page<AlertHistory> findByServerRoomIdInAndTriggeredAtAfterAndTargetTypeNot(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("startTime") LocalDateTime startTime,
            @Param("excludeTargetType") TargetType excludeTargetType,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM AlertHistory a
            WHERE a.serverRoomId IN :serverRoomIds
              AND a.level = :level
              AND a.triggeredAt >= :startTime
              AND a.targetType <> :excludeTargetType
            """)
    Page<AlertHistory> findByServerRoomIdInAndLevelAndTriggeredAtAfterAndTargetTypeNot(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("level") AlertLevel level,
            @Param("startTime") LocalDateTime startTime,
            @Param("excludeTargetType") TargetType excludeTargetType,
            Pageable pageable
    );

    // 통계
    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds")
    long countByServerRoomIdIn(@Param("serverRoomIds") List<Long> serverRoomIds);

    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds AND a.level = :level")
    long countByServerRoomIdInAndLevel(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("level") AlertLevel level
    );

    @Query("SELECT COUNT(a) FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds AND a.targetType = :targetType")
    long countByServerRoomIdInAndTargetType(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("targetType") TargetType targetType
    );

    // 읽음 처리
    @Modifying
    @Query("""
            UPDATE AlertHistory a
               SET a.isRead = true,
                   a.readAt = :readAt,
                   a.readBy = :readBy
             WHERE a.serverRoomId IN :serverRoomIds
               AND a.isRead = false
            """)
    int markAllAsReadByServerRoomIds(
            @Param("serverRoomIds") List<Long> serverRoomIds,
            @Param("readAt") LocalDateTime readAt,
            @Param("readBy") Long readBy
    );

    @Modifying
    @Query("""
            UPDATE AlertHistory a
               SET a.isRead = true,
                   a.readAt = :readAt,
                   a.readBy = :readBy
             WHERE a.id IN :alertIds
               AND a.isRead = false
            """)
    int markAsReadByIds(
            @Param("alertIds") List<Long> alertIds,
            @Param("readAt") LocalDateTime readAt,
            @Param("readBy") Long readBy
    );

    // 삭제
    @Modifying
    @Query("""
            DELETE FROM AlertHistory a
             WHERE a.id IN :alertIds
               AND a.serverRoomId IN :serverRoomIds
            """)
    int deleteByIdsAndServerRoomIds(
            @Param("alertIds") List<Long> alertIds,
            @Param("serverRoomIds") List<Long> serverRoomIds
    );

    @Modifying
    @Query("DELETE FROM AlertHistory a WHERE a.serverRoomId IN :serverRoomIds")
    int deleteAllByServerRoomIds(@Param("serverRoomIds") List<Long> serverRoomIds);

    // 읽지 않은 알림 개수
    @Query("""
            SELECT COUNT(a)
              FROM AlertHistory a
             WHERE a.serverRoomId IN :serverRoomIds
               AND a.isRead = false
            """)
    long countUnreadByServerRoomIds(@Param("serverRoomIds") List<Long> serverRoomIds);
}
