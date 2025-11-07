package org.example.finalbe.domains.history.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.EntityType;
import org.example.finalbe.domains.common.enumdir.HistoryAction;

import java.time.LocalDateTime;

/**
 * 변경 이력 엔티티
 * 서버실 내 모든 자산(Rack, Equipment, Device)의 변경 이력을 추적
 */
@Entity
@Table(name = "history", indexes = {
        @Index(name = "idx_history_serverroom_time", columnList = "server_room_id, changed_at DESC"),
        @Index(name = "idx_history_serverroom_entity", columnList = "server_room_id, entity_type, entity_id, changed_at DESC"),
        @Index(name = "idx_history_user", columnList = "changed_by, changed_at DESC"),
        @Index(name = "idx_history_action", columnList = "server_room_id, action, changed_at DESC")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class History extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    // === 서버실 정보 (파티션 키) ===
    @Column(name = "server_room_id", nullable = false)
    private Long serverRoomId;

    @Column(name = "server_room_name", length = 200)
    private String serverRoomName; // 조회 성능을 위한 비정규화

    // === 엔티티 정보 ===
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private EntityType entityType; // RACK, EQUIPMENT, DEVICE

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "entity_name", length = 200)
    private String entityName; // 조회 성능을 위한 비정규화

    @Column(name = "entity_code", length = 100)
    private String entityCode; // 엔티티 코드 (예: RACK-001, EQ-123)

    // === 작업 정보 ===
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private HistoryAction action; // CREATE, UPDATE, DELETE, STATUS_CHANGE, MOVE

    // === 변경자 정보 ===
    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Column(name = "changed_by_name", length = 100)
    private String changedByName;

    @Column(name = "changed_by_role", length = 50)
    private String changedByRole; // ADMIN, OPERATOR, VIEWER

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // === 변경 내용 (JSON 형식) ===
    @Column(name = "changed_fields", columnDefinition = "TEXT")
    private String changedFields; // ["status", "location"] 형식의 JSON 배열

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue; // JSON 객체

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue; // JSON 객체

    // === 추가 정보 ===
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // 변경 사유

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // 추가 메타데이터 (JSON)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 자동 생성된 변경 설명
}