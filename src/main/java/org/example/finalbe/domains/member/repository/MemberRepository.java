package org.example.finalbe.domains.member.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * userName과 DelYN으로 회원 조회 (Spring Security 인증용)
     */
    @Query("SELECT m FROM Member m " +
            "LEFT JOIN FETCH m.company " +
            "WHERE m.userName = :userName AND m.delYn = :delYn")
    Optional<Member> findByUserNameAndDelYn(@Param("userName") String userName, @Param("delYn") DelYN delYn);

    /**
     * 아이디 중복 체크
     */
    boolean existsByUserName(String userName);

    /**
     * 이메일 중복 체크
     */
    boolean existsByEmail(String email);

    /**
     * 회사별 활성 회원 목록 조회
     */
    @Query("SELECT m FROM Member m WHERE m.company.id = :companyId AND m.status = 'ACTIVE' ORDER BY m.createdAt DESC")
    List<Member> findActiveByCompanyId(@Param("companyId") Long companyId);

    /**
     * 회사별 회원 목록 조회 (DelYN 기준)
     */
    @Query("SELECT m FROM Member m WHERE m.company.id = :companyId AND m.delYn = :delYn ORDER BY m.createdAt DESC")
    List<Member> findByCompanyIdAndDelYn(@Param("companyId") Long companyId, @Param("delYn") DelYN delYn);

    /**
     * 역할별 회원 목록 조회
     */
    @Query("SELECT m FROM Member m WHERE m.role = :role AND m.delYn = :delYn ORDER BY m.createdAt DESC")
    List<Member> findByRoleAndDelYn(@Param("role") org.example.finalbe.domains.common.enumdir.Role role, @Param("delYn") DelYN delYn);


    /**
     * Member와 Company를 함께 조회 (Fetch Join)
     */
    @Query("SELECT m FROM Member m JOIN FETCH m.company WHERE m.id = :id")
    Optional<Member> findByIdWithCompany(@Param("id") Long id);
}