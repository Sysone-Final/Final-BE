package org.example.finalbe.domains.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 공통 엔티티 추상 클래스
 * 생성 시간, 수정 시간, 삭제 시간, 삭제 여부를 관리
 *
 * 사용 기술:
 * - JPA @MappedSuperclass: 이 클래스를 상속받는 엔티티들이 필드를 공유
 * - Hibernate @CreationTimestamp, @UpdateTimestamp: 자동으로 시간 설정
 * - Soft Delete: 실제 DB에서 삭제하지 않고 삭제 플래그만 변경
 *
 * 이 클래스를 상속받는 모든 엔티티는 자동으로 시간 추적 기능을 가짐
 */
@Getter // Lombok: 모든 필드의 getter 자동 생성
@Setter // Lombok: 모든 필드의 setter 자동 생성
@MappedSuperclass // JPA: 이 클래스를 상속받는 자식 엔티티에 필드 매핑
// @MappedSuperclass는 부모 클래스가 테이블로 생성되지 않음
// 자식 엔티티의 테이블에 이 클래스의 컬럼들이 포함됨
// 예: Member 엔티티가 이 클래스를 상속하면, member 테이블에 created_at, updated_at 등의 컬럼이 생성됨

public abstract class BaseTimeEntity {
    // abstract: 직접 인스턴스화 불가, 상속용 클래스

    // === 생성 시간 ===
    @CreationTimestamp // Hibernate: 엔티티 생성 시 자동으로 현재 시간 설정
    // INSERT 쿼리 실행 시 자동으로 현재 시간이 저장됨

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // Jackson: JSON 직렬화 시 날짜 포맷 지정
    // LocalDateTime을 JSON으로 변환할 때 "2025-10-29 14:30:00" 형식으로 출력
    // 이 어노테이션이 없으면 배열 형태로 출력됨: [2025, 10, 29, 14, 30, 0]

    @Column(name = "created_at", updatable = false, nullable = false)
    // updatable = false: 한 번 설정되면 수정 불가 (생성 시간은 변경되면 안 됨)
    // nullable = false: NOT NULL 제약조건 (생성 시간은 필수)
    private LocalDateTime createdAt = LocalDateTime.now();
    // LocalDateTime: Java 8의 날짜/시간 API (구 Date보다 안전하고 편리)
    // 기본값으로 현재 시간 설정 (@CreationTimestamp와 중복이지만 안전장치)

    // === 수정 시간 ===
    @UpdateTimestamp // Hibernate: 엔티티 수정 시 자동으로 현재 시간 업데이트
    // UPDATE 쿼리 실행 시 자동으로 현재 시간으로 갱신됨

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // JSON 포맷 지정

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    // 수정 시간은 처음에는 null이고, 첫 수정 시 값이 설정됨
    // 이후 엔티티가 변경될 때마다 자동으로 업데이트

    // === 삭제 시간 ===
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // JSON 포맷 지정

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    // Soft Delete: 실제로 DELETE 쿼리를 실행하지 않음
    // 삭제 시 이 필드에 현재 시간을 설정하여 "삭제됨"을 표시
    // 장점: 데이터 복구 가능, 삭제 이력 추적 가능
    // 단점: 실제로 데이터가 남아있어 저장 공간 차지

    // === 삭제 여부 ===
    @Enumerated(EnumType.STRING) // Enum을 문자열로 저장 (Y 또는 N)
    @Column(name = "del_yn", nullable = false)
    private DelYN delYn = DelYN.N;
    // DelYN: 삭제 여부 Enum (Y: 삭제됨, N: 삭제 안 됨)
    // 기본값 N: 생성 시에는 삭제되지 않은 상태
    // deletedAt과 함께 사용하여 Soft Delete 구현

    /**
     * Soft Delete 실행
     * 실제로 DB에서 삭제하지 않고, 삭제 플래그만 변경
     *
     * 사용 시점:
     * - 사용자가 회원 탈퇴 요청
     * - 관리자가 데이터 삭제 요청
     * - 일정 기간 후 자동 삭제 (배치 작업)
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now(); // 삭제 시간을 현재 시간으로 설정
        this.delYn = DelYN.Y; // 삭제 플래그를 Y로 변경

        // 이후 조회 쿼리에서 del_yn = 'N' 조건을 추가하여 삭제된 데이터 제외
        // 예: SELECT * FROM member WHERE del_yn = 'N'
    }

    /**
     * 삭제 여부 확인
     *
     * @return 삭제되었으면 true, 아니면 false
     *
     * 사용 예시:
     * if (member.isDeleted()) {
     *     throw new IllegalStateException("삭제된 회원입니다.");
     * }
     */
    public boolean isDeleted() {
        return this.delYn == DelYN.Y; // delYn이 Y면 삭제된 것
    }

    /**
     * 수정 시간 갱신
     * @UpdateTimestamp가 자동으로 처리하지만, 명시적으로 호출이 필요한 경우 사용
     *
     * 사용 시점:
     * - 특정 필드만 변경하고 수정 시간을 명시적으로 갱신하고 싶을 때
     * - @UpdateTimestamp가 동작하지 않는 특수한 경우
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now(); // 수정 시간을 현재 시간으로 설정
    }

    // === 사용 예시 ===

    // Member 엔티티가 BaseTimeEntity를 상속받으면:
    // @Entity
    // public class Member extends BaseTimeEntity {
    //     @Id
    //     private Long id;
    //     private String userName;
    //     // ...
    // }

    // 생성:
    // Member member = new Member();
    // memberRepository.save(member);
    // → created_at: 2025-10-29 14:30:00
    // → del_yn: N

    // 수정:
    // member.setName("홍길동");
    // memberRepository.save(member);
    // → updated_at: 2025-10-29 15:00:00 (자동 갱신)

    // 삭제:
    // member.softDelete();
    // memberRepository.save(member);
    // → deleted_at: 2025-10-29 16:00:00
    // → del_yn: Y

    // 조회 (삭제된 데이터 제외):
    // @Query("SELECT m FROM Member m WHERE m.delYn = 'N'")
    // List<Member> findAllActive();
}