package org.example.finalbe.domains.company.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.dto.*;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 회사 관리 서비스
 *
 * - @Service: Spring의 Service 계층 빈으로 등록
 * - @Transactional: 트랜잭션 관리 (데이터 일관성 보장)
 * - @Slf4j: 로깅 기능 (log.info(), log.error() 등 사용 가능)
 */
@Slf4j // Lombok의 로깅 기능
@Service // Spring의 Service Layer Bean으로 등록
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 (성능 최적화)
// readOnly = true: Dirty Checking을 하지 않아 성능 향상
// 조회 메서드는 읽기 전용, 생성/수정/삭제 메서드만 @Transactional로 오버라이드
public class CompanyService {

    // === 의존성 주입 (생성자 주입) ===
    private final CompanyRepository companyRepository; // 회사 데이터 접근
    private final CompanyServerRoomRepository companyServerRoomRepository; // 회사-전산실 매핑 데이터 접근

    /**
     * 회사 목록 조회 (삭제되지 않은 것만)
     * 시스템에 등록된 모든 활성 회사를 조회
     */
    public List<CompanyListResponse> getAllCompanies() {
        // 로그 출력: 메서드 시작
        log.info("Fetching all active companies");

        // === 1단계: Repository에서 삭제되지 않은 회사 목록 조회 ===
        List<Company> companies = companyRepository.findByDelYn(DelYN.N);
        // findByDelYn(DelYN.N): delYn이 N인 회사만 조회 (Soft Delete 적용)
        // SQL: SELECT * FROM company WHERE del_yn = 'N'

        // 로그 출력: 조회된 회사 수
        log.info("Found {} active companies", companies.size());

        // === 2단계: Entity List → DTO List 변환 ===
        return companies.stream() // Stream API로 List 처리
                .map(CompanyListResponse::from) // 각 Company를 CompanyListResponse로 변환
                // 메서드 참조: company -> CompanyListResponse.from(company)와 동일
                .collect(Collectors.toList()); // Stream을 List로 수집
        // 최종 반환: List<CompanyListResponse>
    }

    /**
     * 회사 상세 조회
     * 특정 회사의 모든 정보를 조회
     */
    public CompanyDetailResponse getCompanyById(Long id) {
        // 로그 출력: 조회 시도
        log.info("Fetching company by id: {}", id);

        // === 1단계: 입력값 검증 ===
        if (id == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        // === 2단계: Repository에서 회사 조회 ===
        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("회사", id));
        // findActiveById(): delYn = N인 회사만 조회
        // Optional의 orElseThrow(): 결과가 없으면 예외 발생
        // EntityNotFoundException: 커스텀 예외 (전역 예외 처리기에서 처리)

        // === 3단계: Entity → DTO 변환 후 반환 ===
        return CompanyDetailResponse.from(company);
        // DetailResponse는 모든 필드를 포함 (상세 조회)
    }

    /**
     * 회사 생성
     * 새로운 회사를 시스템에 등록
     */
    @Transactional // 쓰기 작업이므로 readOnly = false (기본값)
    // @Transactional은 메서드 실행 전에 트랜잭션 시작, 정상 종료 시 commit, 예외 발생 시 rollback
    public CompanyDetailResponse createCompany(CompanyCreateRequest request) {
        // 로그 출력: 회사 생성 시도
        log.info("Creating company with code: {}", request.code());

        // === 1단계: 입력값 검증 ===
        // @Valid로 Controller에서 1차 검증되지만, Service에서도 추가 검증
        if (request.code() == null || request.code().trim().isEmpty()) {
            throw new IllegalArgumentException("회사 코드를 입력해주세요.");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("회사명을 입력해주세요.");
        }

        // === 2단계: 코드 중복 체크 ===
        if (companyRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            // existsByCodeAndDelYn(): 같은 코드의 활성 회사가 있는지 확인
            throw new DuplicateException("회사 코드", request.code());
            // DuplicateException: 커스텀 예외 (409 Conflict 응답)
        }

        // === 3단계: 사업자등록번호 중복 체크 (값이 있는 경우만) ===
        if (request.businessNumber() != null && !request.businessNumber().trim().isEmpty()) {
            // 사업자등록번호가 입력된 경우에만 중복 체크
            if (companyRepository.existsByBusinessNumberAndDelYn(request.businessNumber(), DelYN.N)) {
                throw new DuplicateException("사업자등록번호", request.businessNumber());
            }
        }

        // === 4단계: DTO → Entity 변환 ===
        Company company = request.toEntity();
        // toEntity(): Request DTO의 모든 필드를 Entity로 변환
        // delYn은 Entity의 @Builder.Default로 자동 설정 (DelYN.N)

        // === 5단계: Repository를 통해 DB에 저장 ===
        Company savedCompany = companyRepository.save(company);
        // save(): JPA가 INSERT 쿼리 실행
        // SQL: INSERT INTO company (code, name, ...) VALUES (?, ?, ...)
        // 반환값: ID가 할당된 Company 엔티티

        // 로그 출력: 생성 성공
        log.info("Company created successfully with id: {}", savedCompany.getId());

        // === 6단계: Entity → DTO 변환 후 반환 ===
        return CompanyDetailResponse.from(savedCompany);
        // 생성된 회사의 모든 정보를 DTO로 변환하여 반환
    }

    /**
     * 회사 정보 수정
     * 기존 회사의 정보를 변경
     */
    @Transactional // 쓰기 작업 (UPDATE)
    public CompanyDetailResponse updateCompany(Long id, CompanyUpdateRequest request) {
        // 로그 출력: 수정 시도
        log.info("Updating company with id: {}", id);

        // === 1단계: 입력값 검증 ===
        if (id == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        // === 2단계: Repository에서 회사 조회 ===
        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("회사", id));
        // 수정할 회사가 존재하지 않으면 예외 발생

        // === 3단계: 사업자등록번호 변경 시 중복 체크 ===
        if (request.businessNumber() != null // 사업자등록번호가 입력되었고
                && !request.businessNumber().trim().isEmpty() // 빈 문자열이 아니며
                && !request.businessNumber().equals(company.getBusinessNumber())) { // 기존 값과 다른 경우
            // 다른 회사가 이미 사용 중인 사업자등록번호인지 확인
            if (companyRepository.existsByBusinessNumberAndDelYn(request.businessNumber(), DelYN.N)) {
                throw new DuplicateException("사업자등록번호", request.businessNumber());
            }
        }

        // === 4단계: Entity의 비즈니스 메서드로 수정 ===
        company.updateInfo(
                request.name(),             // 회사명
                request.businessNumber(),   // 사업자등록번호
                request.ceoName(),          // 대표자명
                request.phone(),            // 전화번호
                request.fax(),              // 팩스번호
                request.email(),            // 이메일
                request.address(),          // 주소
                request.website(),          // 웹사이트
                request.industry(),         // 업종
                request.description(),      // 설명
                request.employeeCount(),    // 직원 수
                request.establishedDate(),  // 설립일
                request.logoUrl()           // 로고 URL
        );
        // updateInfo(): null이 아닌 필드만 선택적으로 업데이트 (부분 수정 지원)

        // === 5단계: JPA의 Dirty Checking으로 자동 UPDATE ===
        // save()를 호출하지 않아도 트랜잭션 커밋 시 자동으로 UPDATE 쿼리 실행
        // JPA의 영속성 컨텍스트가 Entity 변경을 감지하여 자동으로 DB 반영
        // SQL: UPDATE company SET name = ?, phone = ?, ... WHERE company_id = ?

        // 로그 출력: 수정 성공
        log.info("Company updated successfully with id: {}", id);

        // === 6단계: Entity → DTO 변환 후 반환 ===
        return CompanyDetailResponse.from(company);
        // 수정된 회사 정보를 DTO로 변환하여 반환
    }

    /**
     * 회사 삭제 (Soft Delete)
     * 실제로 DB에서 삭제하지 않고 delYn을 Y로 변경
     */
    @Transactional // 쓰기 작업 (UPDATE)
    public void deleteCompany(Long id) {
        // 로그 출력: 삭제 시도
        log.info("Deleting company with id: {}", id);

        // === 1단계: 입력값 검증 ===
        if (id == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        // === 2단계: Repository에서 회사 조회 ===
        Company company = companyRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("회사", id));
        // 삭제할 회사가 존재하지 않으면 예외 발생

        // === 3단계: Soft Delete 실행 ===
        company.softDelete();
        // softDelete(): delYn을 Y로 변경
        // 실제 DELETE 쿼리가 아닌 UPDATE 쿼리 실행
        // SQL: UPDATE company SET del_yn = 'Y', updated_at = NOW() WHERE company_id = ?

        // === 4단계: JPA의 Dirty Checking으로 자동 UPDATE ===
        // 트랜잭션 커밋 시 자동으로 DB 반영

        // 로그 출력: 삭제 성공
        log.info("Company soft deleted successfully with id: {}", id);

        // Soft Delete의 장점:
        // 1. 데이터 복구 가능
        // 2. 외래키 참조 무결성 유지 (Member가 Company를 참조)
        // 3. 삭제 이력 추적 가능 (언제 삭제되었는지 확인)
        //
        // 이후 조회 시 WHERE delYn = 'N' 조건을 추가하여 삭제된 데이터 제외
    }

    /**
     * 회사 이름으로 검색
     * 입력한 키워드가 포함된 회사 목록 조회 (부분 일치)
     */
    public List<CompanyListResponse> searchCompaniesByName(String name) {
        // 로그 출력: 검색 시도
        log.info("Searching companies by name: {}", name);

        // === 1단계: 입력값 검증 ===
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        // === 2단계: Repository에서 이름으로 검색 ===
        List<Company> companies = companyRepository.searchByName(name);
        // searchByName(): LIKE 검색으로 부분 일치하는 회사 조회
        // SQL: SELECT * FROM company WHERE name LIKE '%keyword%' AND del_yn = 'N'

        // 로그 출력: 검색 결과 수
        log.info("Found {} companies with name containing: {}", companies.size(), name);

        // === 3단계: Entity List → DTO List 변환 ===
        return companies.stream() // Stream API로 List 처리
                .map(CompanyListResponse::from) // 각 Company를 CompanyListResponse로 변환
                .collect(Collectors.toList()); // Stream을 List로 수집
    }

    /**
     * 회사가 접근 가능한 전산실 목록 조회
     * 특정 회사에 매핑된 모든 전산실(데이터센터) 정보 반환
     */
    public List<CompanyDataCenterListResponse> getCompanyDataCenters(Long companyId) {
        // 로그 출력: 조회 시도
        log.info("Fetching data centers for company: {}", companyId);

        // === 1단계: 입력값 검증 ===
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        // === 2단계: 회사 존재 확인 ===
        companyRepository.findActiveById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("회사", companyId));
        // 회사가 존재하지 않으면 예외 발생

        // === 3단계: CompanyDataCenter 매핑 조회 및 DTO 변환 ===
        List<CompanyDataCenterListResponse> dataCenters = companyServerRoomRepository.findByCompanyId(companyId)
                .stream() // Stream API로 처리
                .map(cdc -> CompanyDataCenterListResponse.from(
                        cdc.getServerRoom(),    // DataCenter 엔티티
                        cdc.getCreatedAt()      // 매핑 생성 시간 (접근 허용일)
                ))
                .collect(Collectors.toList()); // List로 수집

        // 로그 출력: 조회 결과 수
        log.info("Found {} data centers for company: {}", dataCenters.size(), companyId);

        // === 4단계: DTO List 반환 ===
        return dataCenters;
        // 회사가 접근 가능한 전산실 목록 (전산실 정보 + 접근 허용일)
    }
}