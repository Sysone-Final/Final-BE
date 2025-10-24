package org.example.finalbe.domains.department.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // 활성 부서 조회
    @Query("SELECT d FROM Department d WHERE d.id = :id AND d.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    Optional<Department> findActiveById(@Param("id") Long id);

    // 회사별 부서 목록 조회
    List<Department> findByCompanyIdAndDelYn(Long companyId, DelYN delYn);

    // 부서 코드 중복 체크 (같은 회사 내)
    boolean existsByCompanyIdAndDepartmentCodeAndDelYn(Long companyId, String departmentCode, DelYN delYn);

    // 부서명으로 검색 (같은 회사 내)
    @Query("""
        SELECT d FROM Department d
        WHERE d.company.id = :companyId
        AND d.departmentName LIKE %:keyword%
        AND d.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY d.departmentName
    """)
    List<Department> searchByNameInCompany(@Param("companyId") Long companyId, @Param("keyword") String keyword);
}