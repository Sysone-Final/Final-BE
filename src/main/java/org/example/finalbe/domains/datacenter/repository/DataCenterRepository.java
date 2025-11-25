// src/main/java/org/example/finalbe/domains/datacenter/repository/DataCenterRepository.java

package org.example.finalbe.domains.datacenter.repository;

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
     * 활성 데이터센터 조회 (ID)
     */
    @Query("SELECT dc FROM DataCenter dc WHERE dc.id = :id AND dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    Optional<DataCenter> findActiveById(@Param("id") Long id);

    /**
     * 모든 활성 데이터센터 조회
     */
    List<DataCenter> findByDelYn(DelYN delYn);

    /**
     * 회사별 활성 데이터센터 조회
     */
    @Query("SELECT dc FROM DataCenter dc WHERE dc.company.id = :companyId AND dc.delYn = :delYn")
    List<DataCenter> findByCompanyIdAndDelYn(@Param("companyId") Long companyId, @Param("delYn") DelYN delYn);

    /**
     * 데이터센터 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * 데이터센터명으로 검색 (전체)
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        WHERE dc.name LIKE %:keyword%
        AND dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY dc.name
    """)
    List<DataCenter> searchByName(@Param("keyword") String keyword);

    /**
     * 데이터센터명으로 검색 (회사별)
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        WHERE dc.name LIKE %:keyword%
        AND dc.company.id = :companyId
        AND dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY dc.name
    """)
    List<DataCenter> searchByNameAndCompanyId(@Param("keyword") String keyword, @Param("companyId") Long companyId);
    /**
     * DelYn으로 데이터센터 목록 조회
     */
    List<DataCenter> findAllByDelYn(DelYN delYn);

    /**
     * DelYn으로 데이터센터 개수 조회
     */
    long countByDelYn(DelYN delYn);
}