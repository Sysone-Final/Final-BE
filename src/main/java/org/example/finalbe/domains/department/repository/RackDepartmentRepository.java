package org.example.finalbe.domains.department.repository;

import org.example.finalbe.domains.department.domain.RackDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RackDepartmentRepository extends JpaRepository<RackDepartment, Long> {

    // 랙의 부서 목록 조회
    List<RackDepartment> findByRackId(Long rackId);

    // 부서의 랙 목록 조회
    List<RackDepartment> findByDepartmentId(Long departmentId);

    // 랙의 주 담당 부서 조회
    Optional<RackDepartment> findByRackIdAndIsPrimary(Long rackId, Boolean isPrimary);

    // 랙-부서 매핑 존재 여부 확인
    boolean existsByRackIdAndDepartmentId(Long rackId, Long departmentId);

    // 랙-부서 매핑 조회
    Optional<RackDepartment> findByRackIdAndDepartmentId(Long rackId, Long departmentId);

    // 랙의 모든 부서 매핑 삭제
    void deleteByRackId(Long rackId);

    // 부서의 모든 랙 매핑 삭제
    void deleteByDepartmentId(Long departmentId);

    // 특정 부서가 담당하는 랙 수 조회
    @Query("SELECT COUNT(rd) FROM RackDepartment rd WHERE rd.department.id = :departmentId")
    Long countByDepartmentId(@Param("departmentId") Long departmentId);

    // 부서별 랙 목록 조회 (다대다 조인)
    @Query("""
        SELECT rd.rack FROM RackDepartment rd
        WHERE rd.department.id = :departmentId
        AND rd.rack.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY rd.rack.rackName
    """)
    List<org.example.finalbe.domains.rack.domain.Rack> findRacksByDepartmentId(@Param("departmentId") Long departmentId);
}