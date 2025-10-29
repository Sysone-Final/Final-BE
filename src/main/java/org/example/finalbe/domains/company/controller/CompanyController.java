package org.example.finalbe.domains.company.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.company.dto.*;
import org.example.finalbe.domains.company.service.CompanyService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회사 관리 REST API 컨트롤러
 *
 * - @RestController: @Controller + @ResponseBody (모든 메서드가 JSON 응답 반환)
 * - @RequestMapping: 이 컨트롤러의 모든 API는 /api/companies로 시작
 * - Bean Validation: @Valid로 요청 DTO 자동 검증
 * - 선언적 보안: @PreAuthorize로 권한 체크
 *
 * Controller 계층의 역할:
 * 1. HTTP 요청 받기 (Request Mapping)
 * 2. 요청 데이터 검증 (Validation)
 * 3. Service 계층 호출
 * 4. HTTP 응답 반환 (Response)
 */
@RestController // @Controller + @ResponseBody: 모든 메서드가 JSON 응답 반환
@RequestMapping("/api/companies") // 이 컨트롤러의 모든 API는 /api/companies로 시작
// 예: GET /api/companies, POST /api/companies, GET /api/companies/{id}
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Validated // 메서드 파라미터 검증 활성화 (@Min, @NotBlank 등)
// @Validated를 클래스 레벨에 추가해야 @PathVariable, @RequestParam 검증 가능
public class CompanyController {

    // === 의존성 주입 (생성자 주입) ===
    private final CompanyService companyService; // 회사 비즈니스 로직 처리

    /**
     * 시스템에 등록된 모든 회사 목록을 조회하는 기능
     * 회사 이름, ID 등의 기본 정보 목록 제공
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * GET /api/companies
     */
    @GetMapping // HTTP GET 메서드와 매핑 (조회)
    // @GetMapping: @RequestMapping(method = RequestMethod.GET)의 축약형
    // URL: /api/companies (기본 경로)
    public ResponseEntity<CommonResDto> getAllCompanies() {
        // ResponseEntity: HTTP 상태 코드와 바디를 포함하는 응답 객체
        // CommonResDto: 상태 코드, 메시지, 데이터를 포함하는 표준 응답 형식

        // === 1단계: Service 계층 호출 ===
        List<CompanyListResponse> companies = companyService.getAllCompanies();
        // getAllCompanies(): 삭제되지 않은 모든 회사를 조회
        // 반환값: List<CompanyListResponse> (회사 목록 DTO)

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK (성공)
                "회사 목록 조회 완료", // 성공 메시지
                companies // 응답 데이터 (회사 목록)
        );
        // CommonResDto 구조: { status: 200, message: "...", data: [...] }

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(response); // 200 OK와 함께 응답 반환
        // ResponseEntity.ok(): status(200)과 body를 설정한 ResponseEntity 생성
    }

    /**
     * 특정 회사의 상세 정보를 조회하는 기능
     * 회사명, 주소, 연락처, 설립일 등의 자세한 정보 제공
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * GET /api/companies/{id}
     *
     * @param id 회사 ID (Path Variable)
     */
    @GetMapping("/{id}") // HTTP GET + 경로 변수
    // {id}: 경로 변수 (Path Variable) - URL의 일부로 전달되는 값
    // 예: GET /api/companies/1 → id = 1
    public ResponseEntity<CommonResDto> getCompanyById(
            @PathVariable // URL 경로의 {id}를 메서드 파라미터로 바인딩
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // 최소값 검증
            // @Min: ID는 1 이상이어야 함 (0 이하는 유효하지 않음)
            Long id // 회사 ID
    ) {
        // === 1단계: Service 계층 호출 ===
        CompanyDetailResponse company = companyService.getCompanyById(id);
        // getCompanyById(): 특정 회사의 모든 정보를 조회
        // 회사가 없으면 EntityNotFoundException 발생 (전역 예외 처리기에서 404 응답)

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                "회사 조회 완료", // 성공 메시지
                company // 응답 데이터 (회사 상세 정보)
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(response); // 200 OK
    }

    /**
     * 새로운 회사를 등록하는 기능
     * 회사명, 주소, 연락처 등의 정보를 입력받아 신규 회사 생성
     * 권한: ADMIN만 가능
     *
     * POST /api/companies
     *
     * @param request 회사 생성 요청 DTO (Request Body, Validation 적용)
     */
    @PostMapping // HTTP POST 메서드와 매핑 (생성)
    // POST: 새로운 리소스 생성
    @PreAuthorize("hasRole('ADMIN')") // 메서드 레벨 권한 체크
    // @PreAuthorize: Spring Security의 선언적 보안
    // hasRole('ADMIN'): ADMIN 권한을 가진 사용자만 접근 가능
    // ADMIN이 아니면 403 Forbidden 응답
    public ResponseEntity<CommonResDto> createCompany(
            @Valid // Bean Validation 적용 (DTO의 @NotBlank, @Size 등 검증)
            // @Valid: 요청 DTO의 제약조건 검증, 위반 시 400 Bad Request
            @RequestBody // HTTP 요청 바디의 JSON을 DTO 객체로 변환
            // @RequestBody: Content-Type: application/json 필수
            CompanyCreateRequest request // 회사 생성 요청 DTO
    ) {
        // === 1단계: Service 계층 호출 ===
        CompanyDetailResponse company = companyService.createCompany(request);
        // createCompany(): 새로운 회사를 DB에 저장
        // 코드 또는 사업자등록번호 중복 시 DuplicateException 발생 (409 Conflict)

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED, // 201 Created (리소스 생성 성공)
                "회사 생성 완료", // 성공 메시지
                company // 응답 데이터 (생성된 회사 정보)
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        // 201 Created 상태 코드와 함께 응답 반환
        // ResponseEntity.status(): 특정 상태 코드 지정
    }

    /**
     * 기존 회사의 정보를 수정하는 기능
     * 회사명, 주소, 연락처 등을 변경
     * 권한: ADMIN만 가능
     *
     * PUT /api/companies/{id}
     *
     * @param id 회사 ID (Path Variable)
     * @param request 회사 수정 요청 DTO (Request Body, Validation 적용)
     */
    @PutMapping("/{id}") // HTTP PUT 메서드와 경로 변수
    // PUT: 기존 리소스 전체 수정
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한 필요
    public ResponseEntity<CommonResDto> updateCompany(
            @PathVariable // URL 경로의 {id}를 메서드 파라미터로 바인딩
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // ID 검증
            Long id, // 수정할 회사 ID

            @Valid // Bean Validation 적용
            @RequestBody // JSON → DTO 변환
            CompanyUpdateRequest request // 회사 수정 요청 DTO
    ) {
        // === 1단계: Service 계층 호출 ===
        CompanyDetailResponse company = companyService.updateCompany(id, request);
        // updateCompany(): 회사 정보를 수정
        // 회사가 없으면 EntityNotFoundException (404), 중복 시 DuplicateException (409)

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK (수정 성공)
                "회사 수정 완료", // 성공 메시지
                company // 응답 데이터 (수정된 회사 정보)
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(response); // 200 OK
    }

    /**
     * 회사를 삭제하는 기능
     * 회사 정보를 시스템에서 제거 (Soft Delete)
     * 주의: 회사에 속한 직원이나 전산실 매핑이 있으면 삭제가 제한될 수 있음
     * 권한: ADMIN만 가능
     *
     * DELETE /api/companies/{id}
     *
     * @param id 회사 ID (Path Variable)
     */
    @DeleteMapping("/{id}") // HTTP DELETE 메서드와 경로 변수
    // DELETE: 리소스 삭제
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한 필요
    public ResponseEntity<CommonResDto> deleteCompany(
            @PathVariable // URL 경로의 {id}를 메서드 파라미터로 바인딩
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // ID 검증
            Long id // 삭제할 회사 ID
    ) {
        // === 1단계: Service 계층 호출 ===
        companyService.deleteCompany(id);
        // deleteCompany(): Soft Delete (delYn을 Y로 변경)
        // 실제 DB에서 삭제하지 않음

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK (삭제 성공)
                "회사 삭제 완료", // 성공 메시지
                null // 삭제는 반환 데이터 없음
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(response); // 200 OK
    }

    /**
     * 회사 이름으로 검색하는 기능
     * 입력한 키워드가 포함된 이름을 가진 회사 목록 반환 (부분 일치)
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * GET /api/companies/search?name=키워드
     *
     * @param name 검색 키워드 (Query Parameter)
     */
    @GetMapping("/search") // HTTP GET + /search 경로
    // Query Parameter: URL에 ?name=value 형식으로 전달
    // 예: GET /api/companies/search?name=테크
    public ResponseEntity<CommonResDto> searchCompanies(
            @RequestParam // Query Parameter를 메서드 파라미터로 바인딩
            // @RequestParam: URL의 ?name=value에서 값 추출
            @NotBlank(message = "검색어를 입력해주세요.") // 빈 문자열 불허
            // @NotBlank: null, "", "   " 모두 불허
            String name // 검색 키워드
    ) {
        // === 1단계: Service 계층 호출 ===
        List<CompanyListResponse> companies = companyService.searchCompaniesByName(name);
        // searchCompaniesByName(): LIKE 검색으로 부분 일치하는 회사 조회
        // 예: "테크" 검색 → "테크놀로지", "핀테크" 등 모두 조회

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                "회사 검색 완료", // 성공 메시지
                companies // 응답 데이터 (검색된 회사 목록)
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(response); // 200 OK
    }

    /**
     * 특정 회사가 접근할 수 있는 전산실 목록을 조회하는 기능
     * 해당 회사와 매핑된 모든 전산실 정보 제공
     * 회사가 사용 가능한 데이터센터를 확인할 때 사용
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * GET /api/companies/{id}/datacenters
     *
     * @param id 회사 ID (Path Variable)
     */
    @GetMapping("/{id}/datacenters") // HTTP GET + 경로 변수 + 추가 경로
    // {id}/datacenters: 특정 회사의 전산실 목록
    // 예: GET /api/companies/1/datacenters → 1번 회사의 전산실 목록
    public ResponseEntity<CommonResDto> getCompanyDataCenters(
            @PathVariable // URL 경로의 {id}를 메서드 파라미터로 바인딩
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // ID 검증
            Long id // 회사 ID
    ) {
        // === 1단계: Service 계층 호출 ===
        List<CompanyDataCenterListResponse> datacenters = companyService.getCompanyDataCenters(id);
        // getCompanyDataCenters(): 회사가 접근 가능한 전산실 목록 조회
        // CompanyDataCenter 매핑 테이블을 조회하여 DataCenter 정보 추출

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                "회사 전산실 목록 조회 완료", // 성공 메시지
                datacenters // 응답 데이터 (전산실 목록 + 접근 허용일)
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(response); // 200 OK
    }

}