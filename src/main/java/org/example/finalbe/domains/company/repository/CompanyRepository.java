package org.example.finalbe.domains.company.repository;

import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Company 데이터 접근 계층
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    /**
     * 활성 회사 목록 조회
     */
    List<Company> findByDelYn(DelYN delYn);

    /**
     * ID로 활성 회사 조회
     */
    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    Optional<Company> findActiveById(@Param("id") Long id);

    /**
     * 회사 코드 중복 체크
     */
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    /**
     * 사업자등록번호 중복 체크
     */
    boolean existsByBusinessNumberAndDelYn(String businessNumber, DelYN delYn);

    /**
     * 회사명으로 검색 (부분 일치)
     */
    @Query("SELECT c FROM Company c WHERE c.name LIKE %:name% AND c.delYn = org.example.finalbe.domains.common.enumdir.DelYN.N")
    List<Company> searchByName(@Param("name") String name);
}