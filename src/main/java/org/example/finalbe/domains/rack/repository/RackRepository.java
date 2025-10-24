package org.example.finalbe.domains.rack.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.RackStatus;
import org.example.finalbe.domains.rack.domain.Rack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RackRepository extends JpaRepository<Rack, Long> {

    // 삭제되지 않은 랙 조회
    @Query("SELECT r FROM Rack r WHERE r.id = :id AND r.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    Optional<Rack> findActiveById(@Param("id") Long id);

    // 전산실별 랙 목록 조회
    List<Rack> findByDatacenterIdAndDelYn(Long datacenterId, DelYN delYn);

    // 랙 이름 중복 체크 (같은 전산실 내)
    boolean existsByDatacenterIdAndRackNameAndDelYn(Long datacenterId, String rackName, DelYN delYn);

    // 상태별 조회
    @Query("SELECT r FROM Rack r WHERE r.status = :status AND r.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    List<Rack> findByStatus(@Param("status") RackStatus status);


    // 담당자별 조회
    List<Rack> findByManagerIdAndDelYn(Long managerId, DelYN delYn);

    // 키워드 검색 (전체)
    @Query("""
        SELECT r FROM Rack r
        WHERE (r.rackName LIKE %:keyword% 
            OR r.groupNumber LIKE %:keyword% 
            OR r.rackLocation LIKE %:keyword%)
        AND r.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY r.rackName
    """)
    List<Rack> searchByKeyword(@Param("keyword") String keyword);

    // 키워드 검색 (특정 전산실 내)
    @Query("""
        SELECT r FROM Rack r
        WHERE r.datacenter.id = :datacenterId
        AND (r.rackName LIKE %:keyword% 
            OR r.groupNumber LIKE %:keyword% 
            OR r.rackLocation LIKE %:keyword%)
        AND r.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY r.rackName
    """)
    List<Rack> searchByKeywordInDataCenter(
            @Param("keyword") String keyword,
            @Param("datacenterId") Long datacenterId);

    // 키워드 검색 (회사가 접근 가능한 전산실 내)
    @Query("""
        SELECT r FROM Rack r
        JOIN r.datacenter dc
        JOIN CompanyDataCenter cdc ON cdc.dataCenter.id = dc.id
        WHERE cdc.company.id = :companyId
        AND (r.rackName LIKE %:keyword% 
            OR r.groupNumber LIKE %:keyword% 
            OR r.rackLocation LIKE %:keyword%)
        AND r.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
        ORDER BY r.rackName
    """)
    List<Rack> searchByKeywordForCompany(
            @Param("keyword") String keyword,
            @Param("companyId") Long companyId);

    // 랙 이름 존재 여부 확인
    boolean existsByRackNameAndDatacenterIdAndDelYn(String rackName, Long datacenterId, DelYN delYn);
}