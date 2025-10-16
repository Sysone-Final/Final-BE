package org.example.finalbe.domains.member.repository;

import org.example.finalbe.domains.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // 활성 사용자 조회
    @Query("""
    SELECT m FROM Member m
    JOIN FETCH m.company
    WHERE m.username = :username
    AND m.delYn = 'N'
    """)
    Optional<Member> findActiveByUsername(@Param("username") String username);

    // ID로 활성 사용자 조회
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.delYn = 'N'")
    Optional<Member> findActiveById(@Param("id") Long id);
}