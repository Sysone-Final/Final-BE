package org.example.finalbe.domains.department.repository;

import org.example.finalbe.domains.department.domain.MemberDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberDepartmentRepository extends JpaRepository<MemberDepartment, Long> {

    // 회원의 부서 목록 조회
    List<MemberDepartment> findByMemberId(Long memberId);

    // 부서의 회원 목록 조회
    List<MemberDepartment> findByDepartmentId(Long departmentId);

    // 회원의 주 부서 조회
    Optional<MemberDepartment> findByMemberIdAndIsPrimary(Long memberId, Boolean isPrimary);

    // 회원-부서 매핑 존재 여부 확인
    boolean existsByMemberIdAndDepartmentId(Long memberId, Long departmentId);

    // 회원-부서 매핑 조회
    Optional<MemberDepartment> findByMemberIdAndDepartmentId(Long memberId, Long departmentId);

    // 회원의 모든 부서 매핑 삭제
    void deleteByMemberId(Long memberId);

    // 부서의 모든 회원 매핑 삭제
    void deleteByDepartmentId(Long departmentId);

    // 특정 부서에 속한 회원 수 조회
    @Query("SELECT COUNT(md) FROM MemberDepartment md WHERE md.department.id = :departmentId")
    Long countByDepartmentId(@Param("departmentId") Long departmentId);
}