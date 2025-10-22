package org.example.finalbe.domains.company.repository;

import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    // 삭제되지 않은 회사만 조회
    List<Company> findByDelYn(DelYN delYn);

    // ID로 조회 (삭제되지 않은 것만)
    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.delYn = 'N'")
    Optional<Company> findActiveById(@Param("id") Long id);

    // 코드로 조회
    @Query("SELECT c FROM Company c WHERE c.code = :code AND c.delYn = 'N'")
    Optional<Company> findByCode(@Param("code") String code);

    // 코드 중복 체크
    boolean existsByCodeAndDelYn(String code, DelYN delYn);

    // 사업자등록번호 중복 체크
    boolean existsByBusinessNumberAndDelYn(String businessNumber, DelYN delYn);

    // 이름으로 검색
    @Query("SELECT c FROM Company c WHERE c.name LIKE %:name% AND c.delYn = 'N'")
    List<Company> searchByName(@Param("name") String name);
}
