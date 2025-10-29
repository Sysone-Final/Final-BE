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
 * 전산실(데이터센터) 레포지토리
 *
 * - Spring Data JPA: JpaRepository를 상속받아 기본 CRUD 자동 제공
 * - @Query: JPQL을 사용한 커스텀 쿼리 정의
 * - Soft Delete: delYn 컬럼을 사용한 논리 삭제 지원
 * - 회사별 접근 권한: CompanyDataCenter 테이블과 조인하여 접근 가능한 전산실만 조회
 */
public interface DataCenterRepository extends JpaRepository<DataCenter, Long> {
    // JpaRepository<DataCenter, Long>: DataCenter 엔티티와 Primary Key 타입(Long)을 지정
    // 기본 제공 메서드: save(), findById(), findAll(), delete() 등

    /**
     * 삭제되지 않은 전산실 목록 조회
     * Spring Data JPA의 메서드 네이밍 규칙을 따라 자동으로 쿼리 생성
     */
    List<DataCenter> findByDelYn(DelYN delYn);
    // findBy + 필드명: SELECT * FROM datacenter WHERE del_yn = ?

    /**
     * ID로 활성 전산실 조회
     * Soft Delete 적용: delYn이 'N'인 것만 조회
     */
    @Query("SELECT dc FROM DataCenter dc WHERE dc.id = :id AND dc.delYn = 'N'")
    // JPQL 사용: 엔티티 기반 쿼리 (DataCenter 엔티티 별칭 dc)
    Optional<DataCenter> findActiveById(@Param("id") Long id);
    // Optional: 결과가 없을 수 있음을 명시적으로 표현 (null 안정성)

    /**
     * 전산실 코드 중복 확인
     * 코드는 UNIQUE 제약조건이 있으므로 중복 체크 필요
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);
    // existsBy: COUNT 쿼리를 실행하여 결과가 있으면 true, 없으면 false 반환

    /**
     * 상태별 전산실 조회
     * ACTIVE, INACTIVE, MAINTENANCE 등 전산실 상태로 필터링
     */
    @Query("SELECT dc FROM DataCenter dc WHERE dc.status = :status AND dc.delYn = 'N'")
    // :status - Named Parameter 사용 (@Param과 매핑)
    List<DataCenter> findByStatus(@Param("status") DataCenterStatus status);

    /**
     * 전산실 이름으로 검색
     * LIKE 검색으로 부분 일치하는 전산실 조회
     */
    @Query("SELECT dc FROM DataCenter dc WHERE dc.name LIKE %:name% AND dc.delYn = 'N'")
    // LIKE %:name%: 전산실 이름에 검색어가 포함된 모든 결과 조회 (대소문자 구분)
    List<DataCenter> searchByName(@Param("name") String name);

    /**
     * 회사가 접근 가능한 전산실 목록 조회
     * CompanyDataCenter 매핑 테이블을 통해 회사-전산실 관계 확인
     * OPERATOR, VIEWER 권한은 자기 회사에 할당된 전산실만 조회 가능
     */
    @Query("""
    SELECT dc FROM DataCenter dc
    JOIN FETCH dc.manager
    JOIN CompanyDataCenter cdc ON dc.id = cdc.dataCenter.id
    WHERE cdc.company.id = :companyId
    AND dc.delYn = 'N'
    AND cdc.delYn = 'N'
    ORDER BY dc.name
    """)
    // JOIN FETCH: N+1 문제 방지를 위해 manager를 한 번에 조회 (EAGER 로딩)
    // JOIN CompanyDataCenter: 회사-전산실 매핑 테이블과 조인
    // ORDER BY dc.name: 전산실 이름 기준 오름차순 정렬
    List<DataCenter> findAccessibleDataCentersByCompanyId(@Param("companyId") Long companyId);

    /**
     * 회사가 특정 전산실에 접근 권한이 있는지 확인
     * 권한 검증 시 사용 (Service 계층에서 호출)
     */
    @Query("""
        SELECT CASE WHEN COUNT(cdc) > 0 THEN true ELSE false END
        FROM CompanyDataCenter cdc
        WHERE cdc.company.id = :companyId
        AND cdc.dataCenter.id = :dataCenterId
        AND cdc.delYn = 'N'
        AND cdc.dataCenter.delYn = 'N'
    """)
    // CASE WHEN: COUNT가 0보다 크면 true, 아니면 false 반환
    // boolean 반환: 접근 가능하면 true, 불가능하면 false
    boolean hasAccessToDataCenter(
            @Param("companyId") Long companyId,
            @Param("dataCenterId") Long dataCenterId
    );
}