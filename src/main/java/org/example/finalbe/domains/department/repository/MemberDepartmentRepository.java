package org.example.finalbe.domains.department.repository;

import org.example.finalbe.domains.department.domain.MemberDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * MemberDepartment 엔티티 Repository (영속성 계층)
 *
 * - Spring Data JPA를 사용한 데이터 접근 계층
 * - JpaRepository<MemberDepartment, Long> 상속 (CRUD 메서드 자동 제공)
 * - Method Naming + JPQL 기반 쿼리 정의
 *
 * Repository 설계 특징:
 * 1. 회원-부서 간 다대다 관계 조회
 * 2. 주 부서 조회 (isPrimary = true)
 * 3. 중복 배치 체크 (existsBy)
 * 4. 부서별 회원 수 집계
 */
public interface MemberDepartmentRepository extends JpaRepository<MemberDepartment, Long> {
    // JpaRepository<MemberDepartment, Long>:
    // - MemberDepartment: 관리할 엔티티 타입
    // - Long: Primary Key 타입
    // - 기본 CRUD 메서드 자동 제공: save(), findById(), findAll(), deleteById() 등

    /**
     * 회원의 주 부서 조회
     *
     * - 특정 회원의 주 부서(isPrimary = true) 조회
     * - 회원은 주 부서를 1개만 가질 수 있음
     * - Optional로 반환 (주 부서가 없을 수도 있음)
     *
     * @param memberId 회원 ID (예: 1)
     * @param isPrimary 주 부서 여부 (항상 true 전달)
     * @return Optional<MemberDepartment> - 주 부서 매핑 정보
     *
     * 실행 쿼리:
     * SELECT md.* FROM member_department md
     * WHERE md.member_id = :memberId AND md.is_primary = :isPrimary
     */
    Optional<MemberDepartment> findByMemberIdAndIsPrimary(Long memberId, Boolean isPrimary);
    // Spring Data JPA Method Naming:
    // - findBy: SELECT 쿼리 생성
    // - MemberId: md.member.id = ? 조건
    // - And: AND 연산자
    // - IsPrimary: md.isPrimary = ? 조건
    // Service에서:
    // Optional<MemberDepartment> primaryDept = repository.findByMemberIdAndIsPrimary(1L, true);
    // primaryDept.ifPresent(md -> System.out.println("주 부서: " + md.getDepartment().getName()));

    /**
     * 회원-부서 매핑 존재 여부 확인
     *
     * - 특정 회원이 특정 부서에 이미 배치되어 있는지 확인
     * - 중복 배치 방지용 (DB Unique 제약 + 어플리케이션 검증)
     * - boolean 반환 (exists 쿼리)
     *
     * @param memberId 회원 ID (예: 1)
     * @param departmentId 부서 ID (예: 1)
     * @return true: 이미 배치됨, false: 배치 안 됨
     *
     * 실행 쿼리:
     * SELECT CASE WHEN COUNT(md.member_department_id) > 0 THEN true ELSE false END
     * FROM member_department md
     * WHERE md.member_id = :memberId AND md.department_id = :departmentId
     */
    boolean existsByMemberIdAndDepartmentId(Long memberId, Long departmentId);
    // Spring Data JPA Method Naming:
    // - existsBy: SELECT COUNT(*) > 0 쿼리 생성 (boolean 반환)
    // - MemberId: md.member.id = ? 조건
    // - And: AND 연산자
    // - DepartmentId: md.department.id = ? 조건
    // Service에서:
    // if (repository.existsByMemberIdAndDepartmentId(1L, 1L)) {
    //     throw new DuplicateException("이미 해당 부서에 배치되어 있습니다.");
    // }

    /**
     * 회원-부서 매핑 조회
     *
     * - 특정 회원의 특정 부서 매핑 정보 조회
     * - 매핑 삭제 또는 수정 시 사용
     * - Optional로 반환 (매핑이 없을 수도 있음)
     *
     * @param memberId 회원 ID (예: 1)
     * @param departmentId 부서 ID (예: 1)
     * @return Optional<MemberDepartment> - 매핑 정보
     *
     * 실행 쿼리:
     * SELECT md.* FROM member_department md
     * WHERE md.member_id = :memberId AND md.department_id = :departmentId
     */
    Optional<MemberDepartment> findByMemberIdAndDepartmentId(Long memberId, Long departmentId);
    // Spring Data JPA Method Naming:
    // - findBy: SELECT 쿼리 생성
    // - MemberId: md.member.id = ? 조건
    // - And: AND 연산자
    // - DepartmentId: md.department.id = ? 조건
    // Service에서:
    // MemberDepartment mapping = repository.findByMemberIdAndDepartmentId(1L, 1L)
    //     .orElseThrow(() -> new NotFoundException("매핑 정보를 찾을 수 없습니다."));
    // repository.delete(mapping); // 부서에서 회원 제거

    /**
     * 특정 부서에 속한 회원 수 조회
     *
     * - 부서의 employeeCount 필드 갱신 시 사용
     * - 집계 쿼리 (COUNT)
     * - JPQL로 직접 작성
     *
     * @param departmentId 부서 ID (예: 1)
     * @return Long - 해당 부서에 속한 회원 수 (예: 5)
     *
     * 실행 쿼리:
     * SELECT COUNT(md.member_department_id)
     * FROM member_department md
     * WHERE md.department_id = :departmentId
     */
    @Query("SELECT COUNT(md) FROM MemberDepartment md WHERE md.department.id = :departmentId")
    // JPQL:
    // - COUNT(md): MemberDepartment 엔티티 개수 세기
    // - md.department.id: 연관관계 탐색 (department_id 조건)
    Long countByDepartmentId(@Param("departmentId") Long departmentId);
    // @Param: JPQL의 :departmentId와 매개변수 매핑
    // Service에서:
    // Long count = repository.countByDepartmentId(1L);
    // department.setEmployeeCount(count.intValue()); // 실시간 집계 반영
    // 주의: employeeCount 필드와 실제 매핑 개수가 불일치할 수 있으므로 주기적 동기화 필요
}