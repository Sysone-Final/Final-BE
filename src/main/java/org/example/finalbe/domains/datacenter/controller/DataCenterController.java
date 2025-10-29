package org.example.finalbe.domains.datacenter.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.service.DataCenterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 전산실(데이터센터) 관리 컨트롤러
 * 전산실 CRUD 및 검색 API 제공
 *
 * - REST API: RESTful 설계 원칙을 따르는 API
 * - Bean Validation: @Valid를 통한 Request DTO 자동 검증
 * - @PreAuthorize: Spring Security의 메서드 레벨 권한 검증
 * - CommonResDto: 통일된 응답 형식 제공
 */
@RestController // @Controller + @ResponseBody: 모든 메서드가 JSON 응답 반환
@RequestMapping("/api/datacenters") // 기본 URL: /api/datacenters
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Validated // 메서드 파라미터 검증 활성화 (@Min, @Max 등)
public class DataCenterController {

    // === 의존성 주입 (생성자 주입) ===
    private final DataCenterService dataCenterService; // 전산실 비즈니스 로직

    /**
     * 사용자가 접근 가능한 전산실 목록 조회
     *
     * - 권한: 모든 인증된 사용자 접근 가능
     * - ADMIN: 모든 전산실 조회
     * - OPERATOR/VIEWER: 자기 회사에 할당된 전산실만 조회
     * - HTTP Method: GET
     * - URL: /api/datacenters
     * - Response: 전산실 목록 (DataCenterListResponse 배열)
     */
    @GetMapping // GET /api/datacenters
    public ResponseEntity<CommonResDto> getAccessibleDataCenters() {
        // === 1단계: Service 계층 호출 ===
        List<DataCenterListResponse> datacenters = dataCenterService.getAccessibleDataCenters();
        // Service에서 권한 검증 및 데이터 조회 수행

        // === 2단계: 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // HTTP 상태 코드: 200 OK
                "전산실 목록 조회 완료", // 성공 메시지
                datacenters // 전산실 목록 데이터
        );

        // === 3단계: ResponseEntity로 감싸서 반환 ===
        return ResponseEntity.ok(response);
        // ResponseEntity.ok(): HTTP 200 OK 상태로 응답
    }

    /**
     * 특정 전산실 상세 조회
     *
     * - 권한: 모든 인증된 사용자 접근 가능 (회사별 접근 제어는 Service에서 처리)
     * - HTTP Method: GET
     * - URL: /api/datacenters/{id}
     * - Path Variable: id (전산실 ID, 1 이상)
     * - Response: 전산실 상세 정보 (DataCenterDetailResponse)
     *
     * @param id 전산실 ID
     */
    @GetMapping("/{id}") // GET /api/datacenters/{id}
    public ResponseEntity<CommonResDto> getDataCenterById(
            @PathVariable // URL 경로에서 id 값을 추출
            @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") // 최소값 1 검증
            Long id // 전산실 ID (Path Variable)
    ) {
        // === 1단계: Service 계층 호출 ===
        DataCenterDetailResponse datacenter = dataCenterService.getDataCenterById(id);
        // Service에서 권한 검증 및 전산실 조회 수행

        // === 2단계: 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // HTTP 상태 코드: 200 OK
                "전산실 조회 완료", // 성공 메시지
                datacenter // 전산실 상세 데이터
        );

        // === 3단계: ResponseEntity로 감싸서 반환 ===
        return ResponseEntity.ok(response);
    }

    /**
     * 새로운 전산실 생성
     *
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - @PreAuthorize: Spring Security의 메서드 레벨 권한 검증
     * - HTTP Method: POST
     * - URL: /api/datacenters
     * - Request Body: DataCenterCreateRequest (JSON)
     * - Response: 생성된 전산실 정보 (DataCenterDetailResponse)
     *
     * @param request 전산실 생성 요청 DTO (Bean Validation 적용)
     */
    @PostMapping // POST /api/datacenters
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // ADMIN 또는 OPERATOR 권한 필요
    // hasAnyRole(): Spring Security의 SpEL 표현식
    // ROLE_ 접두사는 자동으로 추가됨 (hasAnyRole('ADMIN') = hasRole('ROLE_ADMIN'))
    public ResponseEntity<CommonResDto> createDataCenter(
            @Valid // Bean Validation 활성화 (@NotBlank, @Size 등 검증)
            @RequestBody // HTTP Request Body를 DataCenterCreateRequest로 변환
            DataCenterCreateRequest request // 전산실 생성 요청 DTO
    ) {
        // @Valid: 요청 DTO의 유효성 검증 수행
        // 검증 실패 시 MethodArgumentNotValidException 발생 (GlobalExceptionHandler에서 처리)

        // === 1단계: Service 계층 호출 ===
        DataCenterDetailResponse datacenter = dataCenterService.createDataCenter(request);
        // Service에서 권한 검증, 중복 체크, 전산실 생성 수행

        // === 2단계: 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED, // HTTP 상태 코드: 201 Created
                "전산실 생성 완료", // 성공 메시지
                datacenter // 생성된 전산실 데이터
        );

        // === 3단계: ResponseEntity로 감싸서 반환 ===
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
        // ResponseEntity.status(): 커스텀 HTTP 상태 코드 설정
        // 201 Created: 리소스 생성 성공
    }

    /**
     * 기존 전산실 정보 수정
     *
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - HTTP Method: PUT
     * - URL: /api/datacenters/{id}
     * - Path Variable: id (전산실 ID, 1 이상)
     * - Request Body: DataCenterUpdateRequest (JSON)
     * - Response: 수정된 전산실 정보 (DataCenterDetailResponse)
     *
     * @param id 전산실 ID
     * @param request 전산실 수정 요청 DTO (Bean Validation 적용)
     */
    @PutMapping("/{id}") // PUT /api/datacenters/{id}
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // ADMIN 또는 OPERATOR 권한 필요
    public ResponseEntity<CommonResDto> updateDataCenter(
            @PathVariable // URL 경로에서 id 값을 추출
            @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") // 최소값 1 검증
            Long id, // 전산실 ID (Path Variable)

            @Valid // Bean Validation 활성화
            @RequestBody // HTTP Request Body를 DataCenterUpdateRequest로 변환
            DataCenterUpdateRequest request // 전산실 수정 요청 DTO
    ) {
        // === 1단계: Service 계층 호출 ===
        DataCenterDetailResponse datacenter = dataCenterService.updateDataCenter(id, request);
        // Service에서 권한 검증, 중복 체크, 전산실 수정 수행

        // === 2단계: 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // HTTP 상태 코드: 200 OK
                "전산실 수정 완료", // 성공 메시지
                datacenter // 수정된 전산실 데이터
        );

        // === 3단계: ResponseEntity로 감싸서 반환 ===
        return ResponseEntity.ok(response);
    }

    /**
     * 전산실 삭제 (Soft Delete)
     *
     * - 권한: ADMIN 또는 OPERATOR만 가능
     * - 실제로는 소프트 삭제(delYn = Y)로 처리
     * - 주의: 전산실에 속한 랙이나 장비가 있으면 삭제가 제한될 수 있음 (비즈니스 요구사항에 따라 Service에서 검증)
     * - HTTP Method: DELETE
     * - URL: /api/datacenters/{id}
     * - Path Variable: id (전산실 ID, 1 이상)
     * - Response: 삭제 완료 메시지
     *
     * @param id 전산실 ID
     */
    @DeleteMapping("/{id}") // DELETE /api/datacenters/{id}
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')") // ADMIN 또는 OPERATOR 권한 필요
    public ResponseEntity<CommonResDto> deleteDataCenter(
            @PathVariable // URL 경로에서 id 값을 추출
            @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") // 최소값 1 검증
            Long id // 전산실 ID (Path Variable)
    ) {
        // === 1단계: Service 계층 호출 ===
        dataCenterService.deleteDataCenter(id);
        // Service에서 권한 검증, Soft Delete 수행

        // === 2단계: 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // HTTP 상태 코드: 200 OK
                "전산실 삭제 완료", // 성공 메시지
                null // 삭제 성공 시 응답 데이터 없음
        );

        // === 3단계: ResponseEntity로 감싸서 반환 ===
        return ResponseEntity.ok(response);
    }

    /**
     * 전산실 이름으로 검색
     *
     * - 권한: 모든 인증된 사용자 접근 가능
     * - ADMIN: 모든 전산실에서 검색
     * - OPERATOR/VIEWER: 자기 회사에 할당된 전산실에서만 검색
     * - HTTP Method: GET
     * - URL: /api/datacenters/search?name={검색어}
     * - Query Parameter: name (검색어)
     * - Response: 검색 결과 목록 (DataCenterListResponse 배열)
     *
     * @param name 검색어
     */
    @GetMapping("/search") // GET /api/datacenters/search?name=서울
    public ResponseEntity<CommonResDto> searchDataCentersByName(
            @RequestParam("name") // Query Parameter에서 name 값을 추출
            String name // 검색어
    ) {
        // @RequestParam: URL의 Query Parameter를 메서드 파라미터로 바인딩
        // 예: /api/datacenters/search?name=서울 → name = "서울"

        // === 1단계: Service 계층 호출 ===
        List<DataCenterListResponse> searchResults = dataCenterService.searchDataCentersByName(name);
        // Service에서 권한별 검색 수행

        // === 2단계: 응답 DTO 생성 ===
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // HTTP 상태 코드: 200 OK
                "전산실 검색 완료", // 성공 메시지
                searchResults // 검색 결과 데이터
        );

        // === 3단계: ResponseEntity로 감싸서 반환 ===
        return ResponseEntity.ok(response);
    }
}