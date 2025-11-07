package org.example.finalbe.domains.companyserverroom.repository;

import org.example.finalbe.domains.companyserverroom.domain.CompanyServerRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * CompanyServerRoom 데이터 접근 계층
 */
public interface CompanyServerRoomRepository extends JpaRepository<CompanyServerRoom, Long> {

    /**
     * 회사별 전산실 매핑 목록 조회
     */
    @Query("""
    SELECT cdc FROM CompanyServerRoom cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.serverRoom dc
    WHERE cdc.company.id = :companyId 
    AND cdc.delYn = 'N'
    """)
    List<CompanyServerRoom> findByCompanyId(@Param("companyId") Long companyId);

    /**
     * 전산실별 회사 매핑 목록 조회
     */
    @Query("""
    SELECT cdc FROM CompanyServerRoom cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.serverRoom dc
    WHERE cdc.serverRoom.id = :serverRoomId 
    AND cdc.delYn = 'N'
    """)
    List<CompanyServerRoom> findByServerRoomId(@Param("serverRoomId") Long serverRoomId);

    /**
     * 특정 회사-전산실 매핑 조회
     */
    @Query("""
    SELECT cdc FROM CompanyServerRoom cdc
    JOIN FETCH cdc.company c
    JOIN FETCH cdc.serverRoom dc
    WHERE cdc.company.id = :companyId 
    AND cdc.serverRoom.id = :serverRoomId 
    AND cdc.delYn = 'N'
    """)
    Optional<CompanyServerRoom> findByCompanyIdAndServerRoomId(
            @Param("companyId") Long companyId,
            @Param("serverRoomId") Long serverRoomId
    );

    /**
     * 매핑 존재 여부 확인
     */
    @Query("""
    SELECT CASE WHEN COUNT(cdc) > 0 THEN true ELSE false END 
    FROM CompanyServerRoom cdc 
    WHERE cdc.company.id = :companyId 
    AND cdc.serverRoom.id = :dataCenterId 
    AND cdc.delYn = 'N'
    """)
    boolean existsByCompanyIdAndServerRoomId(
            @Param("companyId") Long companyId,
            @Param("serverRoomId") Long serverRoomId
    );
}