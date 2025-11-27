/**
 * 작성자: 황요한
 * 장치 관리 컨트롤러
 */
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

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Validated
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 서버실별 장치 목록 조회
     */
    @GetMapping("/serverroom/{serverRoomId}")
    public ResponseEntity<CommonResDto> getDevicesByServerRoom(
            @PathVariable @Min(value = 1, message = "유효하지 않은 서버실 ID입니다.") Long serverRoomId) {

        ServerRoomDeviceListResponse response = deviceService.getDevicesByServerRoom(serverRoomId);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 목록 조회 완료", response));
    }

    /**
     * 장치 상세 조회
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
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createDevice(
            @Valid @RequestBody DeviceCreateRequest request) {

        DeviceDetailResponse device = deviceService.createDevice(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "장치 생성 완료", device));
    }

    /**
     * 장치 수정
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
     * 장치 상태 변경
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeDeviceStatus(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id,
            @Valid @RequestBody DeviceStatusChangeRequest request) {

        deviceService.changeDeviceStatus(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 상태 변경 완료", null));
    }

    /**
     * 장치 위치 변경
     */
    @PutMapping("/{id}/position")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDevicePosition(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id,
            @Valid @RequestBody DevicePositionUpdateRequest request) {

        deviceService.updateDevicePosition(id, request);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 위치 변경 완료", null));
    }
}