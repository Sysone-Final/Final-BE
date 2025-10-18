package org.example.finalbe.domains.member.repository;

import org.example.finalbe.domains.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUserName(String userName);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);

    // 활성 사용자 조회
    @Query("""
    SELECT m FROM Member m
    JOIN FETCH m.company
    WHERE m.userName = :userName
    AND m.delYn = 'N'
    """)
    Optional<Member> findActiveByUserName(@Param("userName") String userName);

    // ID로 활성 사용자 조회
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.delYn = 'N'")
    Optional<Member> findActiveById(@Param("id") Long id);
}