package org.example.finalbe.domains.rack.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.rack.domain.Rack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Rack 데이터 접근 계층
 */
@Repository
public interface RackRepository extends JpaRepository<Rack, Long> {

    /**
     * ID로 활성 랙 조회 (delYn = N)
     */
    @Query("SELECT r FROM Rack r WHERE r.id = :id AND r.delYn = 'N'")
    Optional<Rack> findActiveById(@Param("id") Long id);

    /**
     * 서버실별 랙 목록 조회
     */
    @Query("SELECT r FROM Rack r WHERE r.serverroom.id = :serverRoomId AND r.delYn = :delYn ORDER BY r.rackName")
    List<Rack> findByServerRoomIdAndDelYn(
            @Param("serverRoomId") Long serverRoomId,
            @Param("delYn") DelYN delYn);

    /**
     * 담당자별 랙 목록 조회
     */
    @Query("SELECT r FROM Rack r WHERE r.managerId = :managerId AND r.delYn = :delYn ORDER BY r.rackName")
    List<Rack> findByManagerIdAndDelYn(
            @Param("managerId") Long managerId,
            @Param("delYn") DelYN delYn);

    /**
     * 상태별 랙 목록 조회
     */
    @Query("SELECT r FROM Rack r WHERE r.status = :status AND r.delYn = :delYn ORDER BY r.rackName")
    List<Rack> findByStatusAndDelYn(
            @Param("status") RackStatus status,
            @Param("delYn") DelYN delYn);

    /**
     * 키워드 검색 (서버실 내)
     */
    @Query("""
        SELECT r FROM Rack r
        WHERE r.serverroom.id = :serverRoomId
        AND (r.rackName LIKE %:keyword% 
            OR r.groupNumber LIKE %:keyword% 
            OR r.rackLocation LIKE %:keyword%)
        AND r.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY r.rackName
    """)
    List<Rack> searchByKeywordInServerRoom(
            @Param("keyword") String keyword,
            @Param("serverRoomId") Long serverRoomId);

    /**
     * 키워드 검색 (회사 접근 가능 범위)
     */
    @Query("""
        SELECT r FROM Rack r
        JOIN r.serverroom sr
        JOIN CompanyServerRoom csr ON csr.serverRoom.id = sr.id
        WHERE csr.company.id = :companyId
        AND (r.rackName LIKE %:keyword% 
            OR r.groupNumber LIKE %:keyword% 
            OR r.rackLocation LIKE %:keyword%)
        AND r.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY r.rackName
    """)
    List<Rack> searchByKeywordForCompany(
            @Param("keyword") String keyword,
            @Param("companyId") Long companyId);

    /**
     * 랙 이름 존재 여부 확인 (서버실별)
     */
    boolean existsByRackNameAndServerroomIdAndDelYn(String rackName, Long serverRoomId, DelYN delYn);
}