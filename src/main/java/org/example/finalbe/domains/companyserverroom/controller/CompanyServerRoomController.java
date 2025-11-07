package org.example.finalbe.domains.companyserverroom.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomCreateRequest;
import org.example.finalbe.domains.companyserverroom.dto.CompanyServerRoomResponse;
import org.example.finalbe.domains.companyserverroom.service.CompanyServerRoomService;
import org.example.finalbe.domains.company.dto.CompanyServerRoomDetailResponse;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 회사-서버실 매핑 관리 컨트롤러
 * 매핑 생성, 조회, 삭제 API 제공
 * (기존 회사-전산실 매핑 컨트롤러)
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
     * 특정 회사의 서버실 매핑 목록 조회 (상세 정보 포함)
     * GET /api/company-serverrooms/company/{companyId}
     *
     * 변경사항:
     * - 기존: CompanyServerRoomResponse (매핑 정보만)
     * - 변경: CompanyServerRoomDetailResponse (서버실 상세 정보 포함)
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<CommonResDto> getMappingsByCompany(
            @PathVariable @Min(value = 1, message = "유효하지 않은 회사 ID입니다.") Long companyId) {

        List<CompanyServerRoomDetailResponse> mappings =
                companyServerRoomService.getCompanyServerRoomDetailsByCompanyId(companyId);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사의 서버실 상세 정보 조회 완료",
                mappings
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 서버실의 회사 매핑 목록 조회
     * GET /api/company-serverrooms/serverroom/{serverRoomId}
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
     * 회사-서버실 매핑 삭제
     * DELETE /api/company-serverrooms/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteMapping(
            @PathVariable @Min(value = 1, message = "유효하지 않은 매핑 ID입니다.") Long id) {

        companyServerRoomService.deleteMapping(id);

        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "회사-서버실 매핑 삭제 완료",
                null
        );

        return ResponseEntity.ok(response);
    }
}