package org.example.finalbe.domains.department.repository;

import org.example.finalbe.domains.department.domain.RackDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * RackDepartment 엔티티 Repository (영속성 계층)
 *
 * - Spring Data JPA를 사용한 데이터 접근 계층
 * - JpaRepository<RackDepartment, Long> 상속 (CRUD 메서드 자동 제공)
 * - Method Naming + JPQL 기반 쿼리 정의
 *
 * Repository 설계 특징:
 * 1. 랙-부서 간 다대다 관계 조회
 * 2. 주 담당 부서 조회 (isPrimary = true)
 * 3. 중복 배정 체크 (existsBy)
 * 4. 부서별 담당 랙 목록 조회
 */
public interface RackDepartmentRepository extends JpaRepository<RackDepartment, Long> {
    // JpaRepository<RackDepartment, Long>:
    // - RackDepartment: 관리할 엔티티 타입
    // - Long: Primary Key 타입
    // - 기본 CRUD 메서드 자동 제공: save(), findById(), findAll(), deleteById() 등

    /**
     * 랙의 주 담당 부서 조회
     *
     * - 특정 랙의 주 담당 부서(isPrimary = true) 조회
     * - 랙은 주 담당 부서를 1개만 가질 수 있음
     * - Optional로 반환 (주 담당 부서가 없을 수도 있음)
     *
     * @param rackId 랙 ID (예: 1)
     * @param isPrimary 주 담당 부서 여부 (항상 true 전달)
     * @return Optional<RackDepartment> - 주 담당 부서 매핑 정보
     *
     * 실행 쿼리:
     * SELECT rd.* FROM rack_department rd
     * WHERE rd.rack_id = :rackId AND rd.is_primary = :isPrimary
     */
    Optional<RackDepartment> findByRackIdAndIsPrimary(Long rackId, Boolean isPrimary);
    // Spring Data JPA Method Naming:
    // - findBy: SELECT 쿼리 생성
    // - RackId: rd.rack.id = ? 조건
    // - And: AND 연산자
    // - IsPrimary: rd.isPrimary = ? 조건
    // Service에서:
    // Optional<RackDepartment> primaryDept = repository.findByRackIdAndIsPrimary(1L, true);
    // primaryDept.ifPresent(rd -> System.out.println("주 담당 부서: " + rd.getDepartment().getName()));

    /**
     * 랙-부서 매핑 존재 여부 확인
     *
     * - 특정 랙이 특정 부서에 이미 배정되어 있는지 확인
     * - 중복 배정 방지용 (DB Unique 제약 + 어플리케이션 검증)
     * - boolean 반환 (exists 쿼리)
     *
     * @param rackId 랙 ID (예: 1)
     * @param departmentId 부서 ID (예: 1)
     * @return true: 이미 배정됨, false: 배정 안 됨
     *
     * 실행 쿼리:
     * SELECT CASE WHEN COUNT(rd.rack_department_id) > 0 THEN true ELSE false END
     * FROM rack_department rd
     * WHERE rd.rack_id = :rackId AND rd.department_id = :departmentId
     */
    boolean existsByRackIdAndDepartmentId(Long rackId, Long departmentId);
    // Spring Data JPA Method Naming:
    // - existsBy: SELECT COUNT(*) > 0 쿼리 생성 (boolean 반환)
    // - RackId: rd.rack.id = ? 조건
    // - And: AND 연산자
    // - DepartmentId: rd.department.id = ? 조건
    // Service에서:
    // if (repository.existsByRackIdAndDepartmentId(1L, 1L)) {
    //     throw new DuplicateException("이미 해당 부서에 배정되어 있습니다.");
    // }

    /**
     * 랙-부서 매핑 조회
     *
     * - 특정 랙의 특정 부서 매핑 정보 조회
     * - 매핑 삭제 또는 수정 시 사용
     * - Optional로 반환 (매핑이 없을 수도 있음)
     *
     * @param rackId 랙 ID (예: 1)
     * @param departmentId 부서 ID (예: 1)
     * @return Optional<RackDepartment> - 매핑 정보
     *
     * 실행 쿼리:
     * SELECT rd.* FROM rack_department rd
     * WHERE rd.rack_id = :rackId AND rd.department_id = :departmentId
     */
    Optional<RackDepartment> findByRackIdAndDepartmentId(Long rackId, Long departmentId);
    // Spring Data JPA Method Naming:
    // - findBy: SELECT 쿼리 생성
    // - RackId: rd.rack.id = ? 조건
    // - And: AND 연산자
    // - DepartmentId: rd.department.id = ? 조건
    // Service에서:
    // RackDepartment mapping = repository.findByRackIdAndDepartmentId(1L, 1L)
    //     .orElseThrow(() -> new NotFoundException("매핑 정보를 찾을 수 없습니다."));
    // repository.delete(mapping); // 랙에서 부서 배정 해제

    /**
     * 특정 부서가 담당하는 랙 수 조회
     *
     * - 부서가 몇 개의 랙을 관리하는지 집계
     * - 집계 쿼리 (COUNT)
     * - JPQL로 직접 작성
     *
     * @param departmentId 부서 ID (예: 1)
     * @return Long - 해당 부서가 담당하는 랙 수 (예: 10)
     *
     * 실행 쿼리:
     * SELECT COUNT(rd.rack_department_id)
     * FROM rack_department rd
     * WHERE rd.department_id = :departmentId
     */
    @Query("SELECT COUNT(rd) FROM RackDepartment rd WHERE rd.department.id = :departmentId")
    // JPQL:
    // - COUNT(rd): RackDepartment 엔티티 개수 세기
    // - rd.department.id: 연관관계 탐색 (department_id 조건)
    Long countByDepartmentId(@Param("departmentId") Long departmentId);
    // @Param: JPQL의 :departmentId와 매개변수 매핑
    // Service에서:
    // Long rackCount = repository.countByDepartmentId(1L);
    // System.out.println("개발팀이 담당하는 랙: " + rackCount + "개");

    /**
     * 부서별 랙 목록 조회 (다대다 조인)
     *
     * - 특정 부서가 담당하는 모든 랙 조회
     * - 삭제되지 않은(delYn = 'N') 랙만 조회
     * - 랙 이름 오름차순 정렬
     * - RackDepartment 중간 테이블을 통해 Rack 엔티티 조회
     *
     * @param departmentId 부서 ID (예: 1)
     * @return List<Rack> - 해당 부서가 담당하는 랙 목록
     *
     * 실행 쿼리:
     * SELECT r.* FROM rack r
     * JOIN rack_department rd ON r.rack_id = rd.rack_id
     * WHERE rd.department_id = :departmentId
     *   AND r.del_yn = 'N'
     * ORDER BY r.rack_name ASC
     */
    @Query("""
        SELECT rd.rack FROM RackDepartment rd
        WHERE rd.department.id = :departmentId
        AND rd.rack.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY rd.rack.rackName
    """)
    // JPQL:
    // - SELECT rd.rack: RackDepartment의 연관관계를 통해 Rack 엔티티 조회
    //   (중간 테이블 → 최종 엔티티 탐색)
    // - rd.department.id: 부서 ID 조건
    // - rd.rack.delYn: 연관관계를 통해 Rack의 삭제 여부 확인
    // - ORDER BY rd.rack.rackName: 랙 이름 오름차순 정렬
    List<org.example.finalbe.domains.rack.domain.Rack> findRacksByDepartmentId(@Param("departmentId") Long departmentId);
    // @Param: JPQL의 :departmentId와 매개변수 매핑
    // 반환 타입: Rack 엔티티 리스트 (RackDepartment가 아님)
    // Service에서:
    // List<Rack> racks = repository.findRacksByDepartmentId(1L);
    // racks.forEach(rack -> System.out.println("담당 랙: " + rack.getRackName()));
}