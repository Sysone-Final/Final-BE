package org.example.finalbe.domains.department.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Department 엔티티 Repository (영속성 계층)
 *
 * - Spring Data JPA를 사용한 데이터 접근 계층
 * - JpaRepository<Department, Long> 상속 (CRUD 메서드 자동 제공)
 * - JPQL + Method Naming 기반 쿼리 정의
 *
 * Repository 설계 특징:
 * 1. Soft Delete 고려 (delYn = 'N'인 데이터만 조회)
 * 2. 회사별 부서 조회 (companyId 기반 필터링)
 * 3. 복합 조건 검색 지원 (부서명, 키워드)
 * 4. 중복 체크 메서드 제공 (부서 코드 중복 방지)
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // JpaRepository<Department, Long>:
    // - Department: 관리할 엔티티 타입
    // - Long: Primary Key 타입
    // - 기본 CRUD 메서드 자동 제공: save(), findById(), findAll(), deleteById() 등

    /**
     * 활성 부서 단건 조회 (ID 기반)
     *
     * - 삭제되지 않은(delYn = 'N') 부서만 조회
     * - Optional로 반환 (null 안전)
     * - @Query로 JPQL 직접 작성 (조건 명시)
     *
     * @param id 조회할 부서 ID (예: 1)
     * @return Optional<Department> - 존재하면 Department, 없으면 empty()
     *
     * 실행 쿼리:
     * SELECT d.* FROM department d
     * WHERE d.department_id = :id AND d.del_yn = 'N'
     */
    @Query("SELECT d FROM Department d WHERE d.id = :id AND d.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    // JPQL: 엔티티와 필드명 사용 (테이블/컬럼명 아님)
    // d.delYn = DelYN.N: Enum 타입 비교 (문자열 'N'으로 변환됨)
    Optional<Department> findActiveById(@Param("id") Long id);
    // @Param: JPQL의 :id와 매개변수 id를 매핑
    // Optional: null 대신 empty() 반환으로 NPE 방지
    // Service에서: departmentRepository.findActiveById(1L).orElseThrow(() -> new NotFoundException("부서 없음"));

    /**
     * 회사별 부서 목록 조회 (활성 부서만)
     *
     * - 특정 회사에 속한 모든 부서 조회
     * - 삭제되지 않은(delYn = 'N') 부서만 조회
     * - Method Naming 기반 쿼리 자동 생성
     *
     * @param companyId 회사 ID (예: 1)
     * @param delYn 삭제 여부 (항상 DelYN.N 전달)
     * @return List<Department> - 조회된 부서 목록 (빈 리스트 가능)
     *
     * 실행 쿼리:
     * SELECT d.* FROM department d
     * WHERE d.company_id = :companyId AND d.del_yn = :delYn
     */
    List<Department> findByCompanyIdAndDelYn(Long companyId, DelYN delYn);
    // Spring Data JPA Method Naming:
    // - findBy: SELECT 쿼리 생성
    // - CompanyId: d.company.id = ? 조건 추가
    // - And: AND 연산자
    // - DelYn: d.delYn = ? 조건 추가
    // Service에서: departmentRepository.findByCompanyIdAndDelYn(1L, DelYN.N);

    /**
     * 부서 코드 중복 체크 (같은 회사 내)
     *
     * - 같은 회사 내에서 동일한 부서 코드가 이미 존재하는지 확인
     * - 삭제되지 않은(delYn = 'N') 부서만 체크
     * - Method Naming 기반 쿼리 자동 생성
     * - boolean 반환 (exists 쿼리)
     *
     * @param companyId 회사 ID (예: 1)
     * @param departmentCode 부서 코드 (예: "DEV")
     * @param delYn 삭제 여부 (항상 DelYN.N 전달)
     * @return true: 이미 존재, false: 존재하지 않음
     *
     * 실행 쿼리:
     * SELECT CASE WHEN COUNT(d.department_id) > 0 THEN true ELSE false END
     * FROM department d
     * WHERE d.company_id = :companyId
     *   AND d.department_code = :departmentCode
     *   AND d.del_yn = :delYn
     */
    boolean existsByCompanyIdAndDepartmentCodeAndDelYn(Long companyId, String departmentCode, DelYN delYn);
    // Spring Data JPA Method Naming:
    // - existsBy: SELECT COUNT(*) > 0 쿼리 생성 (boolean 반환)
    // - CompanyId: d.company.id = ? 조건
    // - And: AND 연산자
    // - DepartmentCode: d.departmentCode = ? 조건
    // - And: AND 연산자
    // - DelYn: d.delYn = ? 조건
    // Service에서:
    // if (departmentRepository.existsByCompanyIdAndDepartmentCodeAndDelYn(1L, "DEV", DelYN.N)) {
    //     throw new DuplicateException("이미 존재하는 부서 코드입니다.");
    // }

    /**
     * 부서명으로 검색 (같은 회사 내)
     *
     * - 특정 회사 내에서 부서명에 키워드가 포함된 부서 검색
     * - 삭제되지 않은(delYn = 'N') 부서만 조회
     * - LIKE 검색 (부분 일치)
     * - 부서명 오름차순 정렬
     *
     * @param companyId 회사 ID (예: 1)
     * @param keyword 검색 키워드 (예: "개발")
     * @return List<Department> - 검색된 부서 목록 (빈 리스트 가능)
     *
     * 실행 쿼리:
     * SELECT d.* FROM department d
     * JOIN company c ON d.company_id = c.company_id
     * WHERE d.company_id = :companyId
     *   AND d.department_name LIKE '%keyword%'
     *   AND d.delYn = 'N'
     * ORDER BY d.department_name ASC
     */
    @Query("""
        SELECT d FROM Department d
        WHERE d.company.id = :companyId
        AND d.departmentName LIKE %:keyword%
        AND d.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY d.departmentName
    """)
    // JPQL:
    // - d.company.id: 연관관계 탐색 (JOIN 자동 생성)
    // - LIKE %:keyword%: 부분 일치 검색 (양쪽 와일드카드)
    //   예: keyword = "개발" → "개발팀", "백엔드 개발팀", "개발 지원팀" 모두 매칭
    // - ORDER BY d.departmentName: 부서명 오름차순 정렬 (가나다순)
    List<Department> searchByNameInCompany(@Param("companyId") Long companyId, @Param("keyword") String keyword);
    // @Param: JPQL의 :companyId, :keyword와 매개변수를 매핑
    // Service에서: departmentRepository.searchByNameInCompany(1L, "개발");
    // 결과: "개발팀", "개발 지원팀", "백엔드 개발팀" 등
}