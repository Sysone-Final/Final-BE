/**
 * 작성자: 황요한
 * CompanyServerRoom 데이터 접근 계층
 */
package org.example.finalbe.domains.companyserverroom.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.companyserverroom.domain.CompanyServerRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyServerRoomRepository extends JpaRepository<CompanyServerRoom, Long> {

    /**
     * 회사별 서버실 매핑 목록 조회
     */
    @Query("""
    SELECT csr FROM CompanyServerRoom csr
    JOIN FETCH csr.company c
    JOIN FETCH csr.serverRoom sr
    LEFT JOIN FETCH sr.dataCenter dc
    WHERE csr.company.id = :companyId 
    AND csr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    AND (dc IS NULL OR dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N)
    """)
    List<CompanyServerRoom> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * 서버실별 회사 매핑 목록 조회
     */
    @Query("""
    SELECT csr FROM CompanyServerRoom csr
    JOIN FETCH csr.company c
    JOIN FETCH csr.serverRoom sr
    LEFT JOIN FETCH sr.dataCenter dc
    WHERE csr.serverRoom.id = :serverRoomId 
    AND csr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    AND (dc IS NULL OR dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N)
    """)
    List<CompanyServerRoom> findByServerRoomId(@Param("serverRoomId") Long serverRoomId);

    /**
     * 특정 회사-서버실 매핑 조회
     */
    @Query("""
    SELECT csr FROM CompanyServerRoom csr
    JOIN FETCH csr.company c
    JOIN FETCH csr.serverRoom sr
    LEFT JOIN FETCH sr.dataCenter dc
    WHERE csr.company.id = :companyId 
    AND csr.serverRoom.id = :serverRoomId 
    AND csr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    AND (dc IS NULL OR dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N)
    """)
    Optional<CompanyServerRoom> findByCompanyIdAndServerRoomId(
            @Param("companyId") Long companyId,
            @Param("serverRoomId") Long serverRoomId
    );

    /**
     * 회사-서버실 매핑 존재 확인
     */
    @Query("""
    SELECT CASE WHEN COUNT(csr) > 0 THEN true ELSE false END
    FROM CompanyServerRoom csr
    JOIN csr.serverRoom sr
    LEFT JOIN sr.dataCenter dc
    WHERE csr.company.id = :companyId
    AND csr.serverRoom.id = :serverRoomId
    AND csr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    AND sr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    AND (dc IS NULL OR dc.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N)
    """)
    boolean existsByCompanyIdAndServerRoomId(
            @Param("companyId") Long companyId,
            @Param("serverRoomId") Long serverRoomId
    );

    /**
     * 회사별 매핑 삭제 (논리 삭제)
     */
    @Query("""
    UPDATE CompanyServerRoom csr
    SET csr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.Y
    WHERE csr.company.id = :companyId
    AND csr.serverRoom.id IN :serverRoomIds
    AND csr.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N
    """)
    int softDeleteByCompanyIdAndServerRoomIds(
            @Param("companyId") Long companyId,
            @Param("serverRoomIds") List<Long> serverRoomIds
    );
}