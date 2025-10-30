package org.example.finalbe.domains.device.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.device.dto.*;
import org.example.finalbe.domains.device.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 장치 관리 컨트롤러
 * 전산실 내 장치의 생성, 조회, 수정, 삭제 API 제공
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Validated
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 전산실별 장치 목록 조회
     * GET /api/devices/datacenter/{dataCenterId}
     *
     * @param dataCenterId 전산실 ID
     * @return 장치 목록
     */
    @GetMapping("/datacenter/{dataCenterId}")
    public ResponseEntity<CommonResDto> getDevicesByDataCenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long dataCenterId) {

        List<DeviceListResponse> devices = deviceService.getDevicesByDatacenter(dataCenterId);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 목록 조회 완료", devices));
    }

    /**
     * 장치 상세 조회
     * GET /api/devices/{id}
     *
     * @param id 장치 ID
     * @return 장치 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getDeviceById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id) {

        DeviceDetailResponse device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 조회 완료", device));
    }

    /**
     * 장치 생성
     * POST /api/devices
     *
     * @param request 장치 생성 요청 DTO
     * @return 생성된 장치 정보
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        DeviceDetailResponse device = deviceService.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "장치 생성 완료", device));
    }

    /**
     * 장치 수정
     * PUT /api/devices/{id}
     *
     * @param id 장치 ID
     * @param request 장치 수정 요청 DTO
     * @return 수정된 장치 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDevice(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id,
            @Valid @RequestBody DeviceUpdateRequest request) {

        DeviceDetailResponse device = deviceService.updateDevice(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 수정 완료", device));
    }

    /**
     * 장치 삭제
     * DELETE /api/devices/{id}
     *
     * @param id 장치 ID
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteDevice(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id) {

        deviceService.deleteDevice(id);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 삭제 완료", null));
    }

    /**
     * 장치 위치 변경
     * PUT /api/devices/{id}/position
     *
     * @param id 장치 ID
     * @param request 위치 변경 요청 DTO
     * @return 위치 변경된 장치 정보
     */
    @PutMapping("/{id}/position")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDevicePosition(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id,
            @Valid @RequestBody DevicePositionUpdateRequest request) {

        DeviceDetailResponse device = deviceService.updateDevicePosition(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 위치 변경 완료", device));
    }

    /**
     * 장치 상태 변경
     * PUT /api/devices/{id}/status
     *
     * @param id 장치 ID
     * @param request 상태 변경 요청 DTO
     * @return 상태 변경된 장치 정보
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeDeviceStatus(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id,
            @Valid @RequestBody DeviceStatusChangeRequest request) {

        DeviceDetailResponse device = deviceService.changeDeviceStatus(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 상태 변경 완료", device));
    }
}