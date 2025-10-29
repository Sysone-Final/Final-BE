package org.example.finalbe.domains.companydatacenter.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.companydatacenter.dto.CompanyDataCenterCreateRequest;
import org.example.finalbe.domains.companydatacenter.dto.CompanyDataCenterResponse;
import org.example.finalbe.domains.companydatacenter.service.CompanyDataCenterService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 회사-전산실 매핑 관리 REST API 컨트롤러
 *
 * 주요 기능:
 * - 매핑 생성/조회/삭제 API 제공
 * - 일괄 생성/삭제 지원
 *
 * 보안:
 * - 생성/삭제: ADMIN만 가능
 * - 조회: 모든 인증된 사용자 가능
 */
@RestController // @Controller + @ResponseBody
@RequestMapping("/api/company-datacenters") // 기본 경로
@RequiredArgsConstructor // final 필드 생성자 주입
@Validated // PathVariable 검증 활성화
public class CompanyDataCenterController {

    private final CompanyDataCenterService companyDataCenterService; // 비즈니스 로직 처리

    /**
     * 회사-전산실 매핑 생성 (일괄 생성)
     * 한 회사에 여러 전산실을 한 번에 매핑
     * 권한: ADMIN만 가능
     *
     * POST /api/company-datacenters
     */
    @PostMapping // HTTP POST
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한 체크
    public ResponseEntity<CommonResDto> createMappings(
            @Valid @RequestBody CompanyDataCenterCreateRequest request) {
        // @Valid: DTO 검증 (companyId, dataCenterIds null/empty 체크)
        // @RequestBody: JSON → DTO 변환

        // Service 호출하여 매핑 생성
        List<CompanyDataCenterResponse> mappings =
                companyDataCenterService.createCompanyDataCenterMappings(request);

        // 공통 응답 DTO 생성
        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED, // 201 Created
                "회사-전산실 매핑 생성 완료",
                mappings // 생성된 매핑 목록
        );

        // HTTP 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 회사의 전산실 매핑 목록 조회
     * 회사가 접근 가능한 전산실 목록 제공
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * GET /api/company-datacenters/company/{companyId}
     */
    @GetMapping("/company/{companyId}") // HTTP GET
    public ResponseEntity<CommonResDto> getMappingsByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId) {
        // @PathVariable: URL의 {companyId}를 파라미터로 바인딩
        // @Min: ID는 1 이상이어야 함

        // Service 호출하여 매핑 목록 조회
        List<CompanyDataCenterResponse> mappings =
                companyDataCenterService.getCompanyDataCentersByCompanyId(companyId);

        // 공통 응답 DTO 생성
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                "회사의 전산실 매핑 조회 완료",
                mappings // 매핑 목록
        );

        // HTTP 응답 반환
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 전산실의 회사 매핑 목록 조회
     * 전산실을 사용하는 회사 목록 제공
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * GET /api/company-datacenters/datacenter/{dataCenterId}
     */
    @GetMapping("/datacenter/{dataCenterId}") // HTTP GET
    public ResponseEntity<CommonResDto> getMappingsByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {
        // @PathVariable: URL의 {dataCenterId}를 파라미터로 바인딩
        // @Min: ID는 1 이상

        // Service 호출하여 매핑 목록 조회
        List<CompanyDataCenterResponse> mappings =
                companyDataCenterService.getCompanyDataCentersByDataCenterId(dataCenterId);

        // 공통 응답 DTO 생성
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                "전산실의 회사 매핑 조회 완료",
                mappings // 매핑 목록
        );

        // HTTP 응답 반환
        return ResponseEntity.ok(response);
    }

    /**
     * 회사-전산실 매핑 삭제 (단건)
     * 특정 회사의 특정 전산실 접근 권한 제거
     * 권한: ADMIN만 가능
     *
     * DELETE /api/company-datacenters/{companyId}/{dataCenterId}
     */
    @DeleteMapping("/{companyId}/{dataCenterId}") // HTTP DELETE
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한 체크
    public ResponseEntity<CommonResDto> deleteMapping(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId,
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {
        // 두 PathVariable 모두 검증

        // Service 호출하여 매핑 삭제 (Soft Delete)
        companyDataCenterService.deleteCompanyDataCenterMapping(companyId, dataCenterId);

        // 공통 응답 DTO 생성
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                "회사-전산실 매핑 삭제 완료",
                null // 삭제는 반환 데이터 없음
        );

        // HTTP 응답 반환
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 회사의 여러 전산실 매핑 일괄 삭제
     * 한 회사의 여러 전산실 접근 권한을 한 번에 제거
     * 권한: ADMIN만 가능
     *
     * DELETE /api/company-datacenters/company/{companyId}/batch
     * Request Body: {"dataCenterIds": [1, 2, 3]}
     */
    @DeleteMapping("/company/{companyId}/batch") // HTTP DELETE
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한 체크
    public ResponseEntity<CommonResDto> deleteCompanyDataCentersByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId,
            @RequestBody Map<String, List<Long>> request) {
        // @RequestBody: JSON에서 Map으로 변환

        // Request Body에서 전산실 ID 목록 추출
        List<Long> dataCenterIds = request.get("dataCenterIds");

        // Service 호출하여 일괄 삭제
        int deletedCount = companyDataCenterService.deleteCompanyDataCentersByCompany(
                companyId, dataCenterIds);

        // 공통 응답 DTO 생성
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                String.format("회사의 전산실 매핑 %d건 삭제 완료", deletedCount), // 삭제 개수 포함
                Map.of("deletedCount", deletedCount) // 삭제 개수 데이터
        );

        // HTTP 응답 반환
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 전산실의 모든 회사 매핑 삭제
     * 전산실 폐쇄 시 모든 회사의 접근 권한 제거
     * 권한: ADMIN만 가능
     *
     * DELETE /api/company-datacenters/datacenter/{dataCenterId}/all
     */
    @DeleteMapping("/datacenter/{dataCenterId}/all") // HTTP DELETE
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한 체크
    public ResponseEntity<CommonResDto> deleteAllCompaniesByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {
        // @PathVariable: URL의 {dataCenterId}를 파라미터로 바인딩

        // Service 호출하여 모든 매핑 삭제
        int deletedCount = companyDataCenterService.deleteAllCompaniesByDataCenter(dataCenterId);

        // 공통 응답 DTO 생성
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                String.format("전산실의 모든 회사 매핑 %d건 삭제 완료", deletedCount),
                Map.of("deletedCount", deletedCount)
        );

        // HTTP 응답 반환
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 전산실의 특정 회사들 매핑 일괄 삭제
     * 한 전산실에서 여러 회사의 접근 권한 제거
     * 권한: ADMIN만 가능
     *
     * DELETE /api/company-datacenters/datacenter/{dataCenterId}/batch
     * Request Body: {"companyIds": [1, 2, 3]}
     */
    @DeleteMapping("/datacenter/{dataCenterId}/batch") // HTTP DELETE
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한 체크
    public ResponseEntity<CommonResDto> deleteCompaniesByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId,
            @RequestBody Map<String, List<Long>> request) {
        // Request Body에서 회사 ID 목록 추출
        List<Long> companyIds = request.get("companyIds");

        // Service 호출하여 일괄 삭제
        int deletedCount = companyDataCenterService.deleteCompaniesByDataCenter(
                dataCenterId, companyIds);

        // 공통 응답 DTO 생성
        CommonResDto response = new CommonResDto(
                HttpStatus.OK, // 200 OK
                String.format("전산실의 회사 매핑 %d건 삭제 완료", deletedCount),
                Map.of("deletedCount", deletedCount)
        );

        // HTTP 응답 반환
        return ResponseEntity.ok(response);
    }
}