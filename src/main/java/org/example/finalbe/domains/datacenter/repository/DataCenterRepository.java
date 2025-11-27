/**
 * 작성자: 황요한
 * DataCenter 데이터 접근 계층
 * - 데이터센터 조회, 검색, 중복 확인 등의 기능 제공
 */
package org.example.finalbe.domains.datacenter.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DataCenterRepository extends JpaRepository<DataCenter, Long> {

    /**
     * 활성 데이터센터 단건 조회 (ID 기준)
     * delYn = 'N' 인 경우만 조회
     */
    @Query("""
        SELECT dc FROM DataCenter dc 
        WHERE dc.id = :id 
          AND dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    """)
    Optional<DataCenter> findActiveById(@Param("id") Long id);

    /**
     * delYn 조건으로 데이터센터 목록 조회
     */
    List<DataCenter> findByDelYn(DelYN delYn);

    /**
     * 특정 회사의 활성 데이터센터 목록 조회
     */
    @Query("""
        SELECT dc FROM DataCenter dc 
        WHERE dc.company.id = :companyId 
          AND dc.delYn = :delYn
    """)
    List<DataCenter> findByCompanyIdAndDelYn(@Param("companyId") Long companyId,
                                             @Param("delYn") DelYN delYn);

    /**
     * 데이터센터 코드 중복 체크
     * delYn = 'N' 인 활성 데이터만 검사
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * 데이터센터명으로 전체 검색 (부분 일치)
     * delYn = 'N' 인 활성 데이터만 조회
     */
    @Query("""
        SELECT dc FROM DataCenter dc 
        WHERE dc.name LIKE %:keyword% 
          AND dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY dc.name
    """)
    List<DataCenter> searchByName(@Param("keyword") String keyword);

    /**
     * 특정 회사 내 데이터센터명 검색 (부분 일치)
     * delYn = 'N' 인 활성 데이터만 조회
     */
    @Query("""
        SELECT dc FROM DataCenter dc 
        WHERE dc.name LIKE %:keyword%
          AND dc.company.id = :companyId
          AND dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY dc.name
    """)
    List<DataCenter> searchByNameAndCompanyId(@Param("keyword") String keyword,
                                              @Param("companyId") Long companyId);

    /**
     * delYn 상태로 전체 목록 조회
     */
    List<DataCenter> findAllByDelYn(DelYN delYn);

    /**
     * delYn 조건에 따른 데이터센터 개수 조회
     */
    long countByDelYn(DelYN delYn);
}
