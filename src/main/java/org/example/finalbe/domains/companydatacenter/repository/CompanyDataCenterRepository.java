package org.example.finalbe.domains.companydatacenter.repository;

import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * CompanyDataCenter 데이터 접근 계층
 * Spring Data JPA로 기본 CRUD + 커스텀 쿼리 제공
 */
public interface CompanyDataCenterRepository extends JpaRepository<CompanyDataCenter, Long> {

    /**
     * 회사별 전산실 매핑 목록 조회
     * JOIN FETCH로 N+1 문제 방지
     */
    @Query("""
    SELECT cdc FROM CompanyDataCenter cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.dataCenter dc
    WHERE cdc.company.id = :companyId 
    AND cdc.delYn = 'N'
    """)
    // JPQL로 CompanyDataCenter + Company + DataCenter를 한 번에 조회
    // JOIN FETCH: 연관 엔티티를 즉시 로딩하여 N+1 문제 방지
    List<CompanyDataCenter> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * 전산실별 회사 매핑 목록 조회
     * JOIN FETCH로 N+1 문제 방지
     */
    @Query("""
    SELECT cdc FROM CompanyDataCenter cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.dataCenter dc
    WHERE cdc.dataCenter.id = :dataCenterId 
    AND cdc.delYn = 'N'
    """)
    // 전산실 ID로 매핑 조회 + 연관 엔티티 함께 로딩
    List<CompanyDataCenter> findByDataCenterId(@Param("dataCenterId") Long dataCenterId);

    /**
     * 특정 회사-전산실 매핑 조회
     * JOIN FETCH로 연관 엔티티 함께 조회
     */
    @Query("""
    SELECT cdc FROM CompanyDataCenter cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.dataCenter dc
    WHERE cdc.company.id = :companyId 
    AND cdc.dataCenter.id = :dataCenterId 
    AND cdc.delYn = 'N'
    """)
    // 회사 ID + 전산실 ID로 특정 매핑 조회
    Optional<CompanyDataCenter> findByCompanyIdAndDataCenterId(
            @Param("companyId") Long companyId,
            @Param("dataCenterId") Long dataCenterId
    );

    /**
     * 매핑 존재 여부 확인 (중복 체크용)
     * COUNT 쿼리로 효율적 검증
     */
    @Query("""
    SELECT CASE WHEN COUNT(cdc) > 0 THEN true ELSE false END 
    FROM CompanyDataCenter cdc 
    WHERE cdc.company.id = :companyId 
    AND cdc.dataCenter.id = :dataCenterId 
    AND cdc.delYn = 'N'
    """)
    // COUNT 쿼리로 존재 여부만 확인 (데이터 가져오지 않음)
    boolean existsByCompanyIdAndDataCenterId(
            @Param("companyId") Long companyId,
            @Param("dataCenterId") Long dataCenterId
    );
}