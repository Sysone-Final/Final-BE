/**
 * 작성자: 황요한
 * 서버실 관리 컨트롤러
 */
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

@RestController
@RequestMapping("/api/serverrooms")
@RequiredArgsConstructor
@Validated
public class ServerRoomController {

    private final ServerRoomService serverRoomService;

    // 접근 가능한 서버실 목록 조회
    @GetMapping
    public ResponseEntity<CommonResDto> getAccessibleServerRooms() {
        List<ServerRoomGroupedByDataCenterResponse> serverRooms =
                serverRoomService.getAccessibleServerRoomsGroupedByDataCenter();
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "서버실 목록 조회 완료", serverRooms));
    }

    // 서버실 생성
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createServerRoom(
            @Valid @RequestBody ServerRoomCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        ServerRoomDetailResponse serverRoom =
                serverRoomService.createServerRoom(request, httpRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "서버실 생성 완료", serverRoom));
    }

    // 서버실 수정
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateServerRoom(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long id,
            @Valid @RequestBody ServerRoomUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        ServerRoomDetailResponse serverRoom =
                serverRoomService.updateServerRoom(id, request);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "서버실 수정 완료", serverRoom));
    }

    // 서버실 삭제
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> deleteServerRoom(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long id
    ) {
        serverRoomService.deleteServerRoom(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "서버실 삭제 완료", null));
    }

    // 서버실 이름 검색
    @GetMapping("/search")
    public ResponseEntity<CommonResDto> searchServerRoomsByName(
            @RequestParam("name") String name
    ) {
        List<ServerRoomListResponse> results =
                serverRoomService.searchServerRoomsByName(name);

        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "서버실 검색 완료", results));
    }
}
