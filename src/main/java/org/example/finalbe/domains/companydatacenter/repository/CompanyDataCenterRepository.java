package org.example.finalbe.domains.companydatacenter.repository;

import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * CompanyDataCenter 데이터 접근 계층
 */
public interface CompanyDataCenterRepository extends JpaRepository<CompanyDataCenter, Long> {

    /**
     * 회사별 전산실 매핑 목록 조회
     */
    @Query("""
    SELECT cdc FROM CompanyDataCenter cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.dataCenter dc
    WHERE cdc.company.id = :companyId 
    AND cdc.delYn = 'N'
    """)
    List<CompanyDataCenter> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * 전산실별 회사 매핑 목록 조회
     */
    @Query("""
    SELECT cdc FROM CompanyDataCenter cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.dataCenter dc
    WHERE cdc.dataCenter.id = :dataCenterId 
    AND cdc.delYn = 'N'
    """)
    List<CompanyDataCenter> findByDataCenterId(@Param("dataCenterId") Long dataCenterId);

    /**
     * 특정 회사-전산실 매핑 조회
     */
    @Query("""
    SELECT cdc FROM CompanyDataCenter cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.dataCenter dc
    WHERE cdc.company.id = :companyId 
    AND cdc.dataCenter.id = :dataCenterId 
    AND cdc.delYn = 'N'
    """)
    Optional<CompanyDataCenter> findByCompanyIdAndDataCenterId(
            @Param("companyId") Long companyId,
            @Param("dataCenterId") Long dataCenterId
    );

    /**
     * 매핑 존재 여부 확인
     */
    @Query("""
    SELECT CASE WHEN COUNT(cdc) > 0 THEN true ELSE false END 
    FROM CompanyDataCenter cdc 
    WHERE cdc.company.id = :companyId 
    AND cdc.dataCenter.id = :dataCenterId 
    AND cdc.delYn = 'N'
    """)
    boolean existsByCompanyIdAndDataCenterId(
            @Param("companyId") Long companyId,
            @Param("dataCenterId") Long dataCenterId
    );
}