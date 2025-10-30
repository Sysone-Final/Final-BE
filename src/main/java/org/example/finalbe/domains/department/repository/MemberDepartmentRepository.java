package org.example.finalbe.domains.department.repository;

import org.example.finalbe.domains.department.domain.MemberDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * MemberDepartment 데이터 접근 계층
 */
public interface MemberDepartmentRepository extends JpaRepository<MemberDepartment, Long> {

    /**
     * 회원의 주 부서 조회
     */
    Optional<MemberDepartment> findByMemberIdAndIsPrimary(Long memberId, Boolean isPrimary);

    /**
     * 회원-부서 매핑 존재 여부 확인
     */
    boolean existsByMemberIdAndDepartmentId(Long memberId, Long departmentId);

    /**
     * 회원-부서 매핑 조회
     */
    Optional<MemberDepartment> findByMemberIdAndDepartmentId(Long memberId, Long departmentId);

    /**
     * 특정 부서에 속한 회원 수 조회
     */
    @Query("SELECT COUNT(md) FROM MemberDepartment md WHERE md.department.id = :departmentId")
    Long countByDepartmentId(@Param("departmentId") Long departmentId);
}