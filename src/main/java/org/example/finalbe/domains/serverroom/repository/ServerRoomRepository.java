// src/main/java/org/example/finalbe/domains/serverroom/repository/ServerRoomRepository.java

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
     * 활성 서버실 목록 조회
     */
    @Query("""
        SELECT sr FROM ServerRoom sr
        WHERE sr.delYn = :delYn
        ORDER BY sr.name
    """)
    List<ServerRoom> findByDelYn(@Param("delYn") DelYN delYn);

    /**
     * ID로 활성 서버실 조회
     */
    @Query("""
        SELECT sr FROM ServerRoom sr
        WHERE sr.id = :id 
        AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    """)
    Optional<ServerRoom> findActiveById(@Param("id") Long id);

    /**
     * 서버실 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * 상태별 서버실 조회
     */
    @Query("""
        SELECT sr FROM ServerRoom sr
        WHERE sr.status = :status 
        AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY sr.name
    """)
    List<ServerRoom> findByStatus(@Param("status") ServerRoomStatus status);

    /**
     * 서버실 이름으로 검색
     */
    @Query("""
        SELECT sr FROM ServerRoom sr
        WHERE sr.name LIKE %:name%
        AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY sr.name
    """)
    List<ServerRoom> searchByName(@Param("name") String name);

    /**
     * 특정 데이터센터에 속한 활성 서버실 목록 조회 (delYn 필터링)
     */
    @Query("""
        SELECT sr FROM ServerRoom sr 
        WHERE sr.dataCenter.id = :dataCenterId 
        AND sr.delYn = :delYn
        ORDER BY sr.name
    """)
    List<ServerRoom> findByDataCenterIdAndDelYn(
            @Param("dataCenterId") Long dataCenterId,
            @Param("delYn") DelYN delYn
    );

    /**
     * 특정 데이터센터의 활성 서버실 개수 조회
     */
    @Query("""
        SELECT COUNT(sr) FROM ServerRoom sr 
        WHERE sr.dataCenter.id = :dataCenterId 
        AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    """)
    long countByDataCenterIdAndDelYn(@Param("dataCenterId") Long dataCenterId);

    /**
     * DelYn으로 서버실 개수 조회
     */
    long countByDelYn(DelYN delYn);

    /**
     * DelYn으로 서버실 목록 조회
     */
    List<ServerRoom> findAllByDelYn(DelYN delYn);

    /**
     * 데이터센터별 서버실 목록 조회 (수정)
     */
    List<ServerRoom> findByDataCenter_IdAndDelYn(Long dataCenterId, DelYN delYn);

}