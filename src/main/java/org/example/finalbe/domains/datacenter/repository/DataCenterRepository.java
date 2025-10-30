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
     * 회사가 접근 가능한 전산실 목록 조회
     */
    @Query("""
    SELECT dc FROM DataCenter dc
    JOIN FETCH dc.manager
    JOIN CompanyDataCenter cdc ON dc.id = cdc.dataCenter.id
    WHERE cdc.company.id = :companyId
    AND dc.delYn = 'N'
    AND cdc.delYn = 'N'
    ORDER BY dc.name
    """)
    List<DataCenter> findAccessibleDataCentersByCompanyId(@Param("companyId") Long companyId);

    /**
     * 회사의 전산실 접근 권한 확인
     */
    @Query("""
        SELECT CASE WHEN COUNT(cdc) > 0 THEN true ELSE false END
        FROM CompanyDataCenter cdc
        WHERE cdc.company.id = :companyId
        AND cdc.dataCenter.id = :dataCenterId
        AND cdc.delYn = 'N'
        AND cdc.dataCenter.delYn = 'N'
    """)
    boolean hasAccessToDataCenter(
            @Param("companyId") Long companyId,
            @Param("dataCenterId") Long dataCenterId
    );
}