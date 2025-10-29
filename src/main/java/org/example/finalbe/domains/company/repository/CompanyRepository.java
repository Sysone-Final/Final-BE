package org.example.finalbe.domains.company.repository;

import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Company 엔티티에 대한 데이터 접근 계층 (Repository)
 *
 * - Spring Data JPA: JpaRepository를 상속하여 기본 CRUD 메서드 자동 제공
 * - JPQL: @Query 어노테이션으로 복잡한 쿼리 작성
 * - Query Method: 메서드 이름 규칙으로 쿼리 자동 생성
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {
    // JpaRepository<엔티티 타입, ID 타입>을 상속하면 기본 CRUD 메서드가 자동 제공됨
    // - save(): 저장
    // - findById(): ID로 조회
    // - findAll(): 전체 조회
    // - deleteById(): 삭제
    // - count(): 개수 조회 등

    /**
     * 삭제되지 않은 회사 목록 조회
     * Soft Delete된 회사는 제외하고 조회
     */
    List<Company> findByDelYn(DelYN delYn);
    // Spring Data JPA의 Query Method: 메서드 이름 규칙으로 쿼리 자동 생성
    // findBy + 필드명: SELECT * FROM company WHERE del_yn = ?
    // 실제 실행되는 SQL: SELECT * FROM company WHERE del_yn = 'N'

    /**
     * ID로 활성 회사 조회 (삭제되지 않은 것만)
     * 회사 상세 조회, 수정, 삭제 시 사용
     */
    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    // JPQL(Java Persistence Query Language): 객체 지향 쿼리 언어
    // FROM Company c: Company 엔티티를 대상으로 조회 (테이블명이 아닌 엔티티명 사용)
    // c.delYn = DelYN.N: Enum 타입 비교 (문자열이 아닌 Enum 상수 사용)
    // 실제 실행되는 SQL: SELECT * FROM company WHERE company_id = ? AND del_yn = 'N'
    Optional<Company> findActiveById(@Param("id") Long id);
    // @Param: JPQL의 :id 파라미터에 메서드 인자를 바인딩
    // Optional: 결과가 없을 수 있음을 명시 (null-safe 처리)

    /**
     * 회사 코드 중복 체크
     * 회사 생성 시 사용 (같은 코드의 회사가 이미 존재하는지 확인)
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);
    // Spring Data JPA의 Query Method: existsBy + 필드명 + And + 필드명
    // boolean을 반환하며, 데이터가 존재하면 true, 없으면 false
    // 실제 실행되는 SQL: SELECT COUNT(*) > 0 FROM company WHERE code = ? AND del_yn = ?
    // 예: existsByCodeAndDelYn("COMP001", DelYN.N) → 코드가 COMP001이고 삭제되지 않은 회사가 있는지 확인

    /**
     * 사업자등록번호 중복 체크
     * 회사 생성 및 수정 시 사용 (같은 사업자등록번호가 이미 존재하는지 확인)
     */
    boolean existsByBusinessNumberAndDelYn(String businessNumber, DelYN delYn);
    // Query Method: existsBy + 필드명 + And + 필드명
    // 사업자등록번호는 법적으로 고유하므로 중복 체크 필요
    // 실제 실행되는 SQL: SELECT COUNT(*) > 0 FROM company WHERE business_number = ? AND del_yn = ?

    /**
     * 회사 이름으로 검색 (부분 일치)
     * 회사 검색 기능에서 사용 (LIKE 검색)
     */
    @Query("SELECT c FROM Company c WHERE c.name LIKE %:name% AND c.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    // JPQL: LIKE 연산자로 부분 일치 검색
    // %:name%: name 파라미터를 양쪽에 %로 감싸서 부분 일치 검색
    // 예: name = "테크" → "테크", "테크놀로지", "핀테크" 모두 검색됨
    // 실제 실행되는 SQL: SELECT * FROM company WHERE name LIKE '%테크%' AND del_yn = 'N'
    List<Company> searchByName(@Param("name") String name);
    // @Param: JPQL의 :name 파라미터에 메서드 인자를 바인딩
    // List<Company>: 여러 건의 결과를 반환 (검색 결과는 0건 이상)


}