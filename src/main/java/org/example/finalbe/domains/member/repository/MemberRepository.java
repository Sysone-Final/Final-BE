package org.example.finalbe.domains.member.repository;

import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Member 데이터 접근 계층
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 활성 회원 조회 (로그인용)
     */
    @Query("SELECT m FROM Member m WHERE m.userName = :userName AND m.status = 'ACTIVE'")
    Optional<Member> findActiveByUserName(@Param("userName") String userName);

    /**
     * 활성 회원 조회 (ID)
     */
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.status = 'ACTIVE'")
    Optional<Member> findActiveById(@Param("id") Long id);

    /**
     * 아이디 중복 체크
     */
    boolean existsByUserName(String userName);

    /**
     * 이메일 중복 체크
     */
    boolean existsByEmail(String email);

    /**
     * Refresh Token으로 활성 회원 조회
     */
    @Query("SELECT m FROM Member m WHERE m.refreshToken = :refreshToken AND m.status = 'ACTIVE'")
    Optional<Member> findByRefreshToken(@Param("refreshToken") String refreshToken);

    /**
     * 회원 ID와 Refresh Token으로 조회 (이중 검증)
     */
    @Query("SELECT m FROM Member m WHERE m.id = :memberId AND m.refreshToken = :refreshToken AND m.status = 'ACTIVE'")
    Optional<Member> findByIdAndRefreshToken(@Param("memberId") Long memberId, @Param("refreshToken") String refreshToken);
}