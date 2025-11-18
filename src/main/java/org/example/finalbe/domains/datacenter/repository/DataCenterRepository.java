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
     * 데이터센터 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * 데이터센터명으로 검색
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        WHERE dc.name LIKE %:keyword%
        AND dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY dc.name
    """)
    List<DataCenter> searchByName(@Param("keyword") String keyword);

}