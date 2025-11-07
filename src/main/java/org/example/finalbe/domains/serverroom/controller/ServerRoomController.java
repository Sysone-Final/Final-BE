package org.example.finalbe.domains.serverroom.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.serverroom.dto.*;
import org.example.finalbe.domains.serverroom.service.ServerRoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 서버실 관리 컨트롤러
 * (기존 전산실 관리 컨트롤러)
 *
 * 변경사항:
 * - GET /api/serverrooms/{id} 엔드포인트 제거
 * - 서버실 상세 정보는 /api/company-serverrooms/company/{companyId} 에서 조회
 */
@RestController
@RequestMapping("/api/serverrooms")
@RequiredArgsConstructor
@Validated
public class ServerRoomController {

    private final ServerRoomService serverRoomService;

    /**
     * 접근 가능한 서버실 목록 조회
     * GET /api/serverrooms
     *
     * @return 서버실 목록
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAccessibleServerRooms() {
        List<ServerRoomListResponse> serverRooms = serverRoomService.getAccessibleServerRooms();
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "서버실 목록 조회 완료", serverRooms));
    }

    // ===== 상세 조회 엔드포인트 제거 =====
    // GET /api/serverrooms/{id} 삭제됨
    // 대신 /api/company-serverrooms/company/{companyId} 사용

    /**
     * 서버실 생성
     * POST /api/serverrooms
     *
     * @param request 서버실 생성 요청 DTO
     * @return 생성된 서버실 정보
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createServerRoom(
            @Valid @RequestBody ServerRoomCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        ServerRoomDetailResponse serverRoom = serverRoomService.createServerRoom(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "서버실 생성 완료", serverRoom));
    }

    /**
     * 서버실 정보 수정
     * PUT /api/serverrooms/{id}
     *
     * @param id 서버실 ID
     * @param request 서버실 수정 요청 DTO
     * @return 수정된 서버실 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateServerRoom(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.")
            Long id,
            @Valid @RequestBody ServerRoomUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        ServerRoomDetailResponse serverRoom = serverRoomService.updateServerRoom(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "서버실 수정 완료", serverRoom));
    }

    /**
     * 서버실 삭제
     * DELETE /api/serverrooms/{id}
     *
     * @param id 서버실 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteServerRoom(
            @PathVariable
            @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.")
            Long id
    ) {
        serverRoomService.deleteServerRoom(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "서버실 삭제 완료", null));
    }

    /**
     * 서버실 이름으로 검색
     * GET /api/serverrooms/search?name={name}
     *
     * @param name 검색 키워드
     * @return 검색된 서버실 목록
     */
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchServerRoomsByName(
            @RequestParam("name") String name
    ) {
        List<ServerRoomListResponse> searchResults = serverRoomService.searchServerRoomsByName(name);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "서버실 검색 완료", searchResults));
    }
}