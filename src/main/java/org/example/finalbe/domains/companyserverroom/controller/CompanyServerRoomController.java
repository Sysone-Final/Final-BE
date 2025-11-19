package org.example.finalbe.domains.companyserverroom.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomCreateRequest;
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomGroupedByDataCenterResponse;
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomResponse;
import org.example.finalbe.domains.companyserverroom.service.CompanyServerRoomService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 회사-서버실 매핑 관리 컨트롤러
 * 매핑 생성, 조회, 삭제 API 제공
 */
@RestController
@RequestMapping("/api/company-serverrooms")
@RequiredArgsConstructor
@Validated
public class CompanyServerRoomController {

    private final CompanyServerRoomService companyServerRoomService;

    /**
     * 회사-서버실 매핑 생성
     * POST /api/company-serverrooms
     *
     * @param request 매핑 생성 요청 DTO (회사 ID, 서버실 ID 목록)
     * @return 생성된 매핑 목록
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> createMappings(
            @Valid @RequestBody CompanyServerRoomCreateRequest request) {

        List<CompanyServerRoomResponse> mappings =
                companyServerRoomService.createCompanyServerRoomMappings(request);

        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED,
                "회사-서버실 매핑 생성 완료",
                mappings
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 회사의 서버실 매핑 목록 조회 (데이터센터별 그룹화)
     * GET /api/company-serverrooms/company/{companyId}
     *
     * @param companyId 회사 ID
     * @return 데이터센터별로 그룹화된 서버실 목록
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<CommonResDto> getMappingsByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId) {

        List<CompanyServerRoomGroupedByDataCenterResponse> mappings =
                companyServerRoomService.getCompanyServerRoomsGroupedByDataCenter(companyId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사의 서버실 매핑 조회 완료",
                mappings
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 서버실의 회사 매핑 목록 조회
     * GET /api/company-serverrooms/serverroom/{serverRoomId}
     *
     * @param serverRoomId 서버실 ID
     * @return 서버실에 매핑된 회사 목록
     */
    @GetMapping("/serverroom/{serverRoomId}")
    public ResponseEntity<CommonResDto> getMappingsByServerRoom(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        List<CompanyServerRoomResponse> mappings =
                companyServerRoomService.getCompanyServerRoomsByServerRoomId(serverRoomId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "서버실의 회사 매핑 조회 완료",
                mappings
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 회사-서버실 매핑 삭제 (단건)
     * DELETE /r
     *
     * @param companyId 회사 ID
     * @param serverRoomId 서버실 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/company/{companyId}/serverroom/{serverRoomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteMapping(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId,
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        companyServerRoomService.deleteCompanyServerRoomMapping(companyId, serverRoomId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사-서버실 매핑 삭제 완료",
                null
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 회사의 여러 서버실 매핑 일괄 삭제
     * DELETE /api/company-serverrooms/company/{companyId}/serverrooms/batch
     *
     * @param companyId 회사 ID
     * @param request 삭제할 서버실 ID 목록
     * @return 삭제된 매핑 개수
     */
    @DeleteMapping("/company/{companyId}/serverrooms/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteMappingsByCompanyBatch(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId,
            @RequestBody Map<String, List<Long>> request) {

        List<Long> serverRoomIds = request.get("serverRoomIds");
        if (serverRoomIds == null || serverRoomIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 서버실을 하나 이상 선택해주세요.");
        }

        int deletedCount = companyServerRoomService.deleteCompanyServerRoomsByCompany(companyId, serverRoomIds);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("%d개의 매핑이 삭제되었습니다.", deletedCount),
                Map.of("deletedCount", deletedCount)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 서버실의 모든 회사 매핑 삭제
     * DELETE /api/company-serverrooms/serverroom/{serverRoomId}/companies/all
     *
     * @param serverRoomId 서버실 ID
     * @return 삭제된 매핑 개수
     */
    @DeleteMapping("/serverroom/{serverRoomId}/companies/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteAllCompaniesByServerRoom(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        int deletedCount = companyServerRoomService.deleteAllCompaniesByServerRoom(serverRoomId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("%d개의 매핑이 삭제되었습니다.", deletedCount),
                Map.of("deletedCount", deletedCount)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 서버실의 특정 회사들 매핑 일괄 삭제
     * DELETE /api/company-serverrooms/serverroom/{serverRoomId}/companies/batch
     *
     * @param serverRoomId 서버실 ID
     * @param request 삭제할 회사 ID 목록
     * @return 삭제된 매핑 개수
     */
    @DeleteMapping("/serverroom/{serverRoomId}/companies/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteCompaniesByServerRoomBatch(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId,
            @RequestBody Map<String, List<Long>> request) {

        List<Long> companyIds = request.get("companyIds");
        if (companyIds == null || companyIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 회사를 하나 이상 선택해주세요.");
        }

        int deletedCount = companyServerRoomService.deleteCompaniesByServerRoom(serverRoomId, companyIds);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                String.format("%d개의 매핑이 삭제되었습니다.", deletedCount),
                Map.of("deletedCount", deletedCount)
        );

        return ResponseEntity.ok(response);
    }
}
