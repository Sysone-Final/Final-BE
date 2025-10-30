package org.example.finalbe.domains.department.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.department.dto.*;
import org.example.finalbe.domains.department.service.DepartmentService;
import org.example.finalbe.domains.rack.dto.RackListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 부서 관리 REST API Controller (프레젠테이션 계층)
 *
 * - 부서(Department) CRUD API 제공
 * - 회원-부서(MemberDepartment) 매핑 API 제공
 * - 랙-부서(RackDepartment) 매핑 API 제공
 * - RESTful 설계 원칙 준수
 * - JWT 기반 인증 + Spring Security 권한 검증
 *
 * Controller 설계 특징:
 * 1. @RestController: JSON 응답 자동 변환 (@ResponseBody 생략)
 * 2. @RequestMapping: 공통 URL prefix (/api/departments)
 * 3. @PreAuthorize: 메서드 레벨 권한 검증 (SpEL 표현식)
 * 4. @Validated: 클래스 레벨 validation 활성화 (@PathVariable, @RequestParam 검증)
 * 5. CommonResDto: 통일된 응답 형식 (status, message, data)
 */
@RestController // @Controller + @ResponseBody (JSON 응답)
// 모든 메서드의 반환값을 HTTP Response Body로 변환 (View 렌더링 없음)
@RequestMapping("/api/departments") // 공통 URL prefix 지정
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Validated // 클래스 레벨 validation 활성화
// @PathVariable, @RequestParam의 제약 조건 검증 활성화
// 예: @PathVariable @Min(1) Long id → 1 미만이면 ConstraintViolationException 발생
public class DepartmentController {

    // === Service 의존성 주입 ===
    private final DepartmentService departmentService; // 부서 비즈니스 로직
    // @RequiredArgsConstructor로 생성자 자동 생성 및 의존성 주입

    /**
     * 회사별 부서 목록 조회
     *
     * - HTTP Method: GET
     * - URL: /api/departments/company/{companyId}
     * - 권한: 모든 인증된 사용자 (JWT 필터에서 검증)
     * - 응답: 부서 목록 (List<DepartmentListResponse>)
     *
     * @param companyId 회사 ID (PathVariable)
     * @return ResponseEntity<CommonResDto> - HTTP 200, 부서 목록
     *
     * URL 예시:
     * - GET /api/departments/company/1
     * - Header: Authorization: Bearer {JWT_TOKEN}
     * - Response: {"status": 200, "message": "부서 목록 조회 완료", "data": [...]}
     */
    @GetMapping("/company/{companyId}") // GET 요청 매핑
    // URL: /api/departments/company/{companyId}
    public ResponseEntity<CommonResDto> getDepartmentsByCompany(
            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // 최소값 검증 (1 이상)
            Long companyId // 회사 ID (예: 1)
            // @PathVariable: /company/{companyId}의 {companyId}를 Long companyId에 바인딩
            // @Min: 1 미만이면 ConstraintViolationException 발생
    ) {
        // === Service 호출 (비즈니스 로직 실행) ===
        List<DepartmentListResponse> departments = departmentService.getDepartmentsByCompany(companyId);
        // getDepartmentsByCompany(): Service 메서드 호출
        // 권한 검증 및 데이터 조회 후 DTO 리스트 반환

        // === 응답 생성 ===
        return ResponseEntity.ok( // HTTP 200 OK 응답
                new CommonResDto(HttpStatus.OK, "부서 목록 조회 완료", departments)
                // CommonResDto: 통일된 응답 형식
                // status: 200, message: "부서 목록 조회 완료", data: departments
        );
        // ResponseEntity: HTTP 상태 코드 + 헤더 + Body를 포함하는 응답 객체
        // @RestController로 인해 CommonResDto가 자동으로 JSON으로 변환됨
    }

    /**
     * 부서 상세 조회
     *
     * - HTTP Method: GET
     * - URL: /api/departments/{id}
     * - 권한: 모든 인증된 사용자
     * - 응답: 부서 상세 정보 (DepartmentDetailResponse)
     *
     * @param id 부서 ID (PathVariable)
     * @return ResponseEntity<CommonResDto> - HTTP 200, 부서 상세 정보
     *
     * URL 예시:
     * - GET /api/departments/1
     * - Response: {"status": 200, "message": "부서 조회 완료", "data": {...}}
     */
    @GetMapping("/{id}") // GET 요청 매핑
    // URL: /api/departments/{id}
    public ResponseEntity<CommonResDto> getDepartmentById(
            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") // 최소값 검증
            Long id // 부서 ID (예: 1)
    ) {
        // === Service 호출 ===
        DepartmentDetailResponse department = departmentService.getDepartmentById(id);
        // getDepartmentById(): 단건 조회 (상세 정보)

        // === 응답 생성 ===
        return ResponseEntity.ok( // HTTP 200 OK
                new CommonResDto(HttpStatus.OK, "부서 조회 완료", department)
        );
    }

    /**
     * 부서 생성
     *
     * - HTTP Method: POST
     * - URL: /api/departments
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - 요청 Body: DepartmentCreateRequest (JSON)
     * - 응답: 생성된 부서 정보 (DepartmentDetailResponse)
     *
     * @param request 부서 생성 요청 DTO (RequestBody)
     * @return ResponseEntity<CommonResDto> - HTTP 201, 생성된 부서 정보
     *
     * 요청 예시:
     * - POST /api/departments
     * - Header: Authorization: Bearer {JWT_TOKEN}
     * - Body: {"departmentCode": "DEV", "departmentName": "개발팀", ...}
     * - Response: {"status": 201, "message": "부서 생성 완료", "data": {...}}
     */
    @PostMapping // POST 요청 매핑
    // URL: /api/departments
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // 권한 검증 (메서드 실행 전)
    // hasAnyRole: ADMIN 또는 OPERATOR 역할을 가진 사용자만 접근 가능
    // 권한 없으면 AccessDeniedException 발생 (HTTP 403 Forbidden)
    public ResponseEntity<CommonResDto> createDepartment(
            @Valid // Bean Validation 수행 (DTO의 제약 조건 검증)
            @RequestBody // HTTP Request Body를 DepartmentCreateRequest 객체로 변환
            DepartmentCreateRequest request
            // @Valid: DTO의 @NotBlank, @Size 등 제약 조건 검증
            // 검증 실패 시 MethodArgumentNotValidException 발생 (HTTP 400 Bad Request)
            // @RequestBody: JSON → DTO 객체 자동 변환 (Jackson 라이브러리)
    ) {
        // === Service 호출 ===
        DepartmentDetailResponse department = departmentService.createDepartment(request);
        // createDepartment(): 부서 생성 및 저장

        // === 응답 생성 ===
        return ResponseEntity.status(HttpStatus.CREATED) // HTTP 201 Created
                // status(): 특정 HTTP 상태 코드 지정
                .body(new CommonResDto(HttpStatus.CREATED, "부서 생성 완료", department));
        // body(): 응답 Body 설정
        // POST 요청의 성공 응답은 201 Created 사용 (200 OK가 아님)
    }

    /**
     * 부서 수정
     *
     * - HTTP Method: PUT
     * - URL: /api/departments/{id}
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - 요청 Body: DepartmentUpdateRequest (JSON)
     * - 응답: 수정된 부서 정보 (DepartmentDetailResponse)
     *
     * @param id 수정할 부서 ID (PathVariable)
     * @param request 부서 수정 요청 DTO (RequestBody)
     * @return ResponseEntity<CommonResDto> - HTTP 200, 수정된 부서 정보
     *
     * 요청 예시:
     * - PUT /api/departments/1
     * - Body: {"departmentName": "백엔드 개발팀", "description": "서버 개발"}
     * - Response: {"status": 200, "message": "부서 수정 완료", "data": {...}}
     */
    @PutMapping("/{id}") // PUT 요청 매핑
    // URL: /api/departments/{id}
    // PUT: 리소스 전체 또는 일부 수정 (RESTful 설계)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // 권한 검증
    public ResponseEntity<CommonResDto> updateDepartment(
            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") // 최소값 검증
            Long id, // 수정할 부서 ID

            @Valid // Bean Validation 수행
            @RequestBody // HTTP Request Body → DTO 변환
            DepartmentUpdateRequest request // 수정 요청 DTO
    ) {
        // === Service 호출 ===
        DepartmentDetailResponse department = departmentService.updateDepartment(id, request);
        // updateDepartment(): 부서 정보 업데이트

        // === 응답 생성 ===
        return ResponseEntity.ok( // HTTP 200 OK
                new CommonResDto(HttpStatus.OK, "부서 수정 완료", department)
        );
        // PUT 요청의 성공 응답은 200 OK 사용
    }

    /**
     * 부서 삭제 (소프트 삭제)
     *
     * - HTTP Method: DELETE
     * - URL: /api/departments/{id}
     * - 권한: ADMIN만 가능 (엄격한 권한 제어)
     * - 응답: 삭제 완료 메시지
     *
     * @param id 삭제할 부서 ID (PathVariable)
     * @return ResponseEntity<CommonResDto> - HTTP 200, 삭제 완료 메시지
     *
     * 요청 예시:
     * - DELETE /api/departments/1
     * - Response: {"status": 200, "message": "부서 삭제 완료", "data": null}
     */
    @DeleteMapping("/{id}") // DELETE 요청 매핑
    // URL: /api/departments/{id}
    @PreAuthorize("hasRole('ADMIN')") // 권한 검증 (ADMIN만 가능)
    // hasRole: 단일 역할 검증 (ADMIN만)
    // OPERATOR는 삭제 불가 (생성/수정만 가능)
    public ResponseEntity<CommonResDto> deleteDepartment(
            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") // 최소값 검증
            Long id // 삭제할 부서 ID
    ) {
        // === Service 호출 ===
        departmentService.deleteDepartment(id);
        // deleteDepartment(): 소프트 삭제 (delYn = 'Y')

        // === 응답 생성 ===
        return ResponseEntity.ok( // HTTP 200 OK
                new CommonResDto(HttpStatus.OK, "부서 삭제 완료", null)
                // data는 null (삭제 시 반환할 데이터 없음)
        );
        // DELETE 요청의 성공 응답은 200 OK 또는 204 No Content
    }

    /**
     * 부서 검색 (회사 내)
     *
     * - HTTP Method: GET
     * - URL: /api/departments/company/{companyId}/search
     * - 권한: 모든 인증된 사용자
     * - Query Parameter: keyword (검색 키워드)
     * - 응답: 검색된 부서 목록 (List<DepartmentListResponse>)
     *
     * @param companyId 회사 ID (PathVariable)
     * @param keyword 검색 키워드 (RequestParam)
     * @return ResponseEntity<CommonResDto> - HTTP 200, 검색된 부서 목록
     *
     * URL 예시:
     * - GET /api/departments/company/1/search?keyword=개발
     * - Response: {"status": 200, "message": "부서 검색 완료", "data": [...]}
     */
    @GetMapping("/company/{companyId}/search") // GET 요청 매핑
    // URL: /api/departments/company/{companyId}/search?keyword=xxx
    public ResponseEntity<CommonResDto> searchDepartments(
            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") // 최소값 검증
            Long companyId, // 회사 ID

            @RequestParam // Query Parameter 추출
            @NotBlank(message = "검색 키워드를 입력해주세요.") // 필수 입력 검증
            String keyword // 검색 키워드 (예: "개발")
            // @RequestParam: ?keyword=xxx에서 xxx를 String keyword에 바인딩
            // @NotBlank: null, 빈 문자열, 공백 문자열 모두 불허
    ) {
        // === Service 호출 ===
        List<DepartmentListResponse> departments = departmentService.searchDepartments(companyId, keyword);
        // searchDepartments(): LIKE 검색 (부분 일치)

        // === 응답 생성 ===
        return ResponseEntity.ok( // HTTP 200 OK
                new CommonResDto(HttpStatus.OK, "부서 검색 완료", departments)
        );
    }

    /**
     * 회원을 부서에 추가 (회원-부서 매핑 생성)
     *
     * - HTTP Method: POST
     * - URL: /api/departments/members
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - 요청 Body: MemberDepartmentCreateRequest (JSON)
     * - 응답: 추가 완료 메시지
     *
     * @param request 회원-부서 매핑 요청 DTO (RequestBody)
     * @return ResponseEntity<CommonResDto> - HTTP 201, 추가 완료 메시지
     *
     * 요청 예시:
     * - POST /api/departments/members
     * - Body: {"memberId": 1, "departmentId": 1, "isPrimary": true}
     * - Response: {"status": 201, "message": "회원이 부서에 추가되었습니다.", "data": null}
     */
    @PostMapping("/members") // POST 요청 매핑
    // URL: /api/departments/members
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // 권한 검증
    public ResponseEntity<CommonResDto> addMemberToDepartment(
            @Valid // Bean Validation 수행
            @RequestBody // HTTP Request Body → DTO 변환
            MemberDepartmentCreateRequest request // 매핑 요청 DTO
    ) {
        // === Service 호출 ===
        departmentService.addMemberToDepartment(request);
        // addMemberToDepartment(): 회원-부서 매핑 생성

        // === 응답 생성 ===
        return ResponseEntity.status(HttpStatus.CREATED) // HTTP 201 Created
                .body(new CommonResDto(HttpStatus.CREATED, "회원이 부서에 추가되었습니다.", null));
        // data는 null (매핑 생성 시 반환할 데이터 없음)
    }

    /**
     * 부서에서 회원 제거 (회원-부서 매핑 삭제)
     *
     * - HTTP Method: DELETE
     * - URL: /api/departments/members/{memberId}/department/{departmentId}
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - 응답: 제거 완료 메시지
     *
     * @param memberId 회원 ID (PathVariable)
     * @param departmentId 부서 ID (PathVariable)
     * @return ResponseEntity<CommonResDto> - HTTP 200, 제거 완료 메시지
     *
     * URL 예시:
     * - DELETE /api/departments/members/1/department/1
     * - Response: {"status": 200, "message": "회원이 부서에서 제거되었습니다.", "data": null}
     */
    @DeleteMapping("/members/{memberId}/department/{departmentId}") // DELETE 요청 매핑
    // URL: /api/departments/members/{memberId}/department/{departmentId}
    // RESTful 설계: 리소스 계층 구조 표현
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // 권한 검증
    public ResponseEntity<CommonResDto> removeMemberFromDepartment(
            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 회원 ID입니다.") // 최소값 검증
            Long memberId, // 회원 ID

            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") // 최소값 검증
            Long departmentId // 부서 ID
    ) {
        // === Service 호출 ===
        departmentService.removeMemberFromDepartment(memberId, departmentId);
        // removeMemberFromDepartment(): 회원-부서 매핑 삭제

        // === 응답 생성 ===
        return ResponseEntity.ok( // HTTP 200 OK
                new CommonResDto(HttpStatus.OK, "회원이 부서에서 제거되었습니다.", null)
        );
    }

    /**
     * 랙을 부서에 추가 (랙-부서 매핑 생성)
     *
     * - HTTP Method: POST
     * - URL: /api/departments/racks
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - 요청 Body: RackDepartmentCreateRequest (JSON)
     * - 응답: 추가 완료 메시지
     *
     * @param request 랙-부서 매핑 요청 DTO (RequestBody)
     * @return ResponseEntity<CommonResDto> - HTTP 201, 추가 완료 메시지
     *
     * 요청 예시:
     * - POST /api/departments/racks
     * - Body: {"rackId": 1, "departmentId": 1, "isPrimary": true}
     * - Response: {"status": 201, "message": "랙이 부서에 추가되었습니다.", "data": null}
     */
    @PostMapping("/racks") // POST 요청 매핑
    // URL: /api/departments/racks
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // 권한 검증
    public ResponseEntity<CommonResDto> addRackToDepartment(
            @Valid // Bean Validation 수행
            @RequestBody // HTTP Request Body → DTO 변환
            RackDepartmentCreateRequest request // 매핑 요청 DTO
    ) {
        // === Service 호출 ===
        departmentService.addRackToDepartment(request);
        // addRackToDepartment(): 랙-부서 매핑 생성

        // === 응답 생성 ===
        return ResponseEntity.status(HttpStatus.CREATED) // HTTP 201 Created
                .body(new CommonResDto(HttpStatus.CREATED, "랙이 부서에 추가되었습니다.", null));
    }

    /**
     * 부서에서 랙 제거 (랙-부서 매핑 삭제)
     *
     * - HTTP Method: DELETE
     * - URL: /api/departments/racks/{rackId}/department/{departmentId}
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - 응답: 제거 완료 메시지
     *
     * @param rackId 랙 ID (PathVariable)
     * @param departmentId 부서 ID (PathVariable)
     * @return ResponseEntity<CommonResDto> - HTTP 200, 제거 완료 메시지
     *
     * URL 예시:
     * - DELETE /api/departments/racks/1/department/1
     * - Response: {"status": 200, "message": "랙이 부서에서 제거되었습니다.", "data": null}
     */
    @DeleteMapping("/racks/{rackId}/department/{departmentId}") // DELETE 요청 매핑
    // URL: /api/departments/racks/{rackId}/department/{departmentId}
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // 권한 검증
    public ResponseEntity<CommonResDto> removeRackFromDepartment(
            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 랙 ID입니다.") // 최소값 검증
            Long rackId, // 랙 ID

            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") // 최소값 검증
            Long departmentId // 부서 ID
    ) {
        // === Service 호출 ===
        departmentService.removeRackFromDepartment(rackId, departmentId);
        // removeRackFromDepartment(): 랙-부서 매핑 삭제

        // === 응답 생성 ===
        return ResponseEntity.ok( // HTTP 200 OK
                new CommonResDto(HttpStatus.OK, "랙이 부서에서 제거되었습니다.", null)
        );
    }

    /**
     * 부서별 랙 목록 조회
     *
     * - HTTP Method: GET
     * - URL: /api/departments/{departmentId}/racks
     * - 권한: 모든 인증된 사용자
     * - 응답: 랙 목록 (List<RackListResponse>)
     *
     * @param departmentId 부서 ID (PathVariable)
     * @return ResponseEntity<CommonResDto> - HTTP 200, 랙 목록
     *
     * URL 예시:
     * - GET /api/departments/1/racks
     * - Response: {"status": 200, "message": "부서별 랙 목록 조회 완료", "data": [...]}
     */
    @GetMapping("/{departmentId}/racks") // GET 요청 매핑
    // URL: /api/departments/{departmentId}/racks
    // RESTful 설계: 부서의 하위 리소스로 랙 목록 표현
    public ResponseEntity<CommonResDto> getRacksByDepartment(
            @PathVariable // URL 경로에서 변수 추출
            @Min(value = 1, message = "유효하지 않은 부서 ID입니다.") // 최소값 검증
            Long departmentId // 부서 ID
    ) {
        // === Service 호출 ===
        List<RackListResponse> racks = departmentService.getRacksByDepartment(departmentId);
        // getRacksByDepartment(): 부서가 담당하는 랙 목록 조회

        // === 응답 생성 ===
        return ResponseEntity.ok( // HTTP 200 OK
                new CommonResDto(HttpStatus.OK, "부서별 랙 목록 조회 완료", racks)
        );
    }
}