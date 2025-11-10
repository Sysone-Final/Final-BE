package org.example.finalbe.domains.serverroom.repository;

import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * ServerRoom 데이터 접근 계층
 */
public interface ServerRoomRepository extends JpaRepository<ServerRoom, Long> {

    /**
     * ★ 수정: 활성 전산실 목록 조회 (LEFT JOIN FETCH 제거)
     */
    @Query("""
        SELECT dc FROM ServerRoom dc
        WHERE dc.delYn = :delYn
        ORDER BY dc.name
    """)
    List<ServerRoom> findByDelYn(@Param("delYn") DelYN delYn);

    /**
     * ★ 수정: ID로 활성 전산실 조회 (LEFT JOIN FETCH 제거)
     */
    @Query("""
        SELECT dc FROM ServerRoom dc
        WHERE dc.id = :id 
        AND dc.delYn = 'N'
    """)
    Optional<ServerRoom> findActiveById(@Param("id") Long id);

    /**
     * 전산실 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * ★ 수정: 상태별 전산실 조회 (LEFT JOIN FETCH 제거)
     */
    @Query("""
        SELECT dc FROM ServerRoom dc
        WHERE dc.status = :status 
        AND dc.delYn = 'N'
        ORDER BY dc.name
    """)
    List<ServerRoom> findByStatus(@Param("status") ServerRoomStatus status);

    /**
     * ★ 수정: 전산실 이름으로 검색 (LEFT JOIN FETCH 제거)
     */
    @Query("""
        SELECT dc FROM ServerRoom dc
        WHERE dc.name LIKE %:name%
        AND dc.delYn = 'N'
        ORDER BY dc.name
    """)
    List<ServerRoom> searchByName(@Param("name") String name);

    /**
     * 특정 데이터센터에 속한 활성 서버실 목록 조회
     */
    @Query("SELECT sr FROM ServerRoom sr WHERE sr.dataCenter.id = :dataCenterId AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    List<ServerRoom> findByDataCenterIdAndDelYn(@Param("dataCenterId") Long dataCenterId);
}