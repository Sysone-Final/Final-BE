package org.example.finalbe.domains.member.repository;

import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Member 엔티티에 대한 데이터 접근 계층
 *
 * - Spring Data JPA: JpaRepository를 상속하여 기본 CRUD 메서드 자동 제공
 * - JPQL: @Query 어노테이션으로 복잡한 쿼리 작성
 */
@Repository // Spring의 Bean으로 등록되어 의존성 주입 가능
public interface MemberRepository extends JpaRepository<Member, Long> { // JpaRepository<엔티티 타입, ID 타입>

    /**
     * 아이디로 활성 회원 조회
     * 로그인 시 사용
     */
    @Query("SELECT m FROM Member m WHERE m.userName = :userName AND m.status = 'ACTIVE'") // JPQL 쿼리 작성
    // JPQL은 객체 지향 쿼리 언어로, 엔티티 객체를 대상으로 쿼리를 작성함
    // 실제 실행 시 Hibernate가 SQL로 변환: SELECT * FROM member WHERE user_name = ? AND status = 'ACTIVE'
    Optional<Member> findActiveByUserName(@Param("userName") String userName); // Optional로 null-safe 처리
    // @Param: JPQL의 :userName 파라미터에 메서드 인자를 바인딩

    /**
     * ID로 활성 회원 조회
     * 토큰 재발급 및 회원 정보 조회 시 사용
     */
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.status = 'ACTIVE'") // 활성 상태인 회원만 조회
    Optional<Member> findActiveById(@Param("id") Long id); // ID로 조회

    /**
     * 아이디 중복 체크
     * 회원가입 시 사용
     */
    boolean existsByUserName(String userName); // Spring Data JPA의 Query Method
    // 메서드 이름 규칙에 따라 자동으로 쿼리 생성: SELECT COUNT(*) > 0 FROM member WHERE user_name = ?
    // existsBy는 boolean을 반환하며, 데이터가 존재하면 true

    /**
     * 이메일 중복 체크
     * 회원가입 시 사용
     */
    boolean existsByEmail(String email); // 이메일로 중복 체크

    /**
     * Refresh Token으로 회원 조회
     * 토큰 재발급 시 사용하여 유효한 Refresh Token을 가진 회원을 찾음
     */
    @Query("SELECT m FROM Member m WHERE m.refreshToken = :refreshToken AND m.status = 'ACTIVE'") // Refresh Token으로 조회
    // 이 쿼리는 저장된 Refresh Token이 일치하고 활성 상태인 회원을 찾음
    Optional<Member> findByRefreshToken(@Param("refreshToken") String refreshToken);

    /**
     * 회원 ID와 Refresh Token으로 조회
     * 토큰 재발급 시 이중 검증용 (ID와 Token 모두 일치해야 함)
     */
    @Query("SELECT m FROM Member m WHERE m.id = :memberId AND m.refreshToken = :refreshToken AND m.status = 'ACTIVE'")
    // 회원 ID와 Refresh Token이 모두 일치하는지 확인 (보안 강화)
    Optional<Member> findByIdAndRefreshToken(@Param("memberId") Long memberId, @Param("refreshToken") String refreshToken);
}