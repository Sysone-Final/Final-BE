/**
 * 작성자: 황요한
 * 공통 엔티티 기반 클래스 (생성/수정/삭제 시간 및 Soft Delete 관리)
 */
package org.example.finalbe.domains.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false)
    private DelYN delYn = DelYN.N;

    // Soft delete 처리
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.delYn = DelYN.Y;
    }

    // 삭제 여부 확인
    public boolean isDeleted() {
        return this.delYn == DelYN.Y;
    }

    // 수정 시간 갱신
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
