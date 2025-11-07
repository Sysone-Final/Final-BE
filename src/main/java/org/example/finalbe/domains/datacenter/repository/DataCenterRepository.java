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
     * ★ 수정: 활성 전산실 목록 조회 (LEFT JOIN FETCH 제거)
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        WHERE dc.delYn = :delYn
        ORDER BY dc.name
    """)
    List<DataCenter> findByDelYn(@Param("delYn") DelYN delYn);

    /**
     * ★ 수정: ID로 활성 전산실 조회 (LEFT JOIN FETCH 제거)
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        WHERE dc.id = :id 
        AND dc.delYn = 'N'
    """)
    Optional<DataCenter> findActiveById(@Param("id") Long id);

    /**
     * 전산실 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * ★ 수정: 상태별 전산실 조회 (LEFT JOIN FETCH 제거)
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        WHERE dc.status = :status 
        AND dc.delYn = 'N'
        ORDER BY dc.name
    """)
    List<DataCenter> findByStatus(@Param("status") DataCenterStatus status);

    /**
     * ★ 수정: 전산실 이름으로 검색 (LEFT JOIN FETCH 제거)
     */
    @Query("""
        SELECT dc FROM DataCenter dc
        WHERE dc.name LIKE %:name%
        AND dc.delYn = 'N'
        ORDER BY dc.name
    """)
    List<DataCenter> searchByName(@Param("name") String name);
}