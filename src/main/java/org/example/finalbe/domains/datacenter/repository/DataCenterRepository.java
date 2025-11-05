package org.example.finalbe.domains.datacenter.repository;

import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * DataCenter 데이터 접근 계층
 */
public interface DataCenterRepository extends JpaRepository<DataCenter, Long> {

    /**
     * 활성 전산실 목록 조회
     */
    List<DataCenter> findByDelYn(DelYN delYn);

    /**
     * ID로 활성 전산실 조회
     */
    @Query("SELECT dc FROM DataCenter dc WHERE dc.id = :id AND dc.delYn = 'N'")
    Optional<DataCenter> findActiveById(@Param("id") Long id);

    /**
     * 전산실 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * 상태별 전산실 조회
     */
    @Query("SELECT dc FROM DataCenter dc WHERE dc.status = :status AND dc.delYn = 'N'")
    List<DataCenter> findByStatus(@Param("status") DataCenterStatus status);

    /**
     * 전산실 이름으로 검색 (부분 일치)
     */
    @Query("SELECT dc FROM DataCenter dc WHERE dc.name LIKE %:name% AND dc.delYn = 'N'")
    List<DataCenter> searchByName(@Param("name") String name);

    /**
     * ★ 추가: 회사별 전산실 목록 조회
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        JOIN FETCH dc.manager
        WHERE dc.company.id = :companyId
        AND dc.delYn = 'N'
        ORDER BY dc.name
    """)
    List<DataCenter> findByCompanyIdAndDelYn(@Param("companyId") Long companyId, DelYN delYn);

    /**
     * ★ 추가: 회사별 전산실 이름 검색
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        WHERE dc.company.id = :companyId
        AND dc.name LIKE %:name%
        AND dc.delYn = 'N'
    """)
    List<DataCenter> searchByNameAndCompanyId(
            @Param("name") String name,
            @Param("companyId") Long companyId
    );

    /**
     * 회사의 전산실 접근 권한 확인
     * 전산실의 company_id와 회사 ID가 일치하는지 체크
     */
    @Query("""
    SELECT CASE WHEN COUNT(dc) > 0 THEN true ELSE false END
    FROM DataCenter dc
    WHERE dc.id = :dataCenterId
    AND dc.company.id = :companyId
    AND dc.delYn = 'N'
""")
    boolean hasAccessToDataCenter(
            @Param("companyId") Long companyId,
            @Param("dataCenterId") Long dataCenterId
    );
}