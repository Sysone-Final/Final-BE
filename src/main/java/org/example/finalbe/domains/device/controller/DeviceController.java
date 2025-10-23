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
 * 장치 컨트롤러
 * 전산실에 배치되는 물리적 장치(server, door, aircon 등) 관리
 */
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Validated
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 전산실별 장치 목록 조회
     * 특정 전산실에 배치된 모든 장치 목록 반환
     * 그리드 위치 기준으로 정렬되어 반환
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * @param datacenterId 전산실 ID
     */
    @GetMapping("/datacenter/{datacenterId}")
    public ResponseEntity<CommonResDto> getDevicesByDatacenter(
            @PathVariable @Min(value = 1, message = "유효하지 않은 전산실 ID입니다.") Long datacenterId) {

        List<DeviceListResponse> devices = deviceService.getDevicesByDatacenter(datacenterId);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "전산실 장치 목록 조회 완료",
                devices
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 장치 상세 조회
     * 특정 장치의 상세 정보 반환
     * 권한: 모든 인증된 사용자 접근 가능
     *
     * @param id 장치 ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonResDto> getDeviceById(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id) {

        DeviceDetailResponse device = deviceService.getDeviceById(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "장치 조회 완료",
                device
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 장치 생성
     * 새로운 장치를 전산실에 추가
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param request 장치 생성 요청 DTO
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> createDevice(@Valid @RequestBody DeviceCreateRequest request) {

        DeviceDetailResponse device = deviceService.createDevice(request);
        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED,
                "장치 생성 완료",
                device
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 장치 수정
     * 기존 장치의 정보 업데이트
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 장치 ID
     * @param request 장치 수정 요청 DTO
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDevice(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id,
            @Valid @RequestBody DeviceUpdateRequest request) {

        DeviceDetailResponse device = deviceService.updateDevice(id, request);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "장치 수정 완료",
                device
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 장치 삭제 (소프트 삭제)
     * 장치를 논리적으로 삭제
     * 권한: ADMIN만 가능
     *
     * @param id 장치 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteDevice(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id) {

        deviceService.deleteDevice(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "장치 삭제 완료",
                null
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 장치 위치 변경
     * 전산실 내에서 장치의 그리드 위치 변경
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 장치 ID
     * @param request 위치 변경 요청 DTO
     */
    @PatchMapping("/{id}/position")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> updateDevicePosition(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id,
            @Valid @RequestBody DevicePositionUpdateRequest request) {

        DeviceDetailResponse device = deviceService.updateDevicePosition(id, request);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "장치 위치 변경 완료",
                device
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 장치 상태 변경
     * 장치의 상태를 변경 (NORMAL, MAINTENANCE, ERROR 등)
     * 권한: ADMIN 또는 OPERATOR만 가능
     *
     * @param id 장치 ID
     * @param request 상태 변경 요청 DTO
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<CommonResDto> changeDeviceStatus(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 ID입니다.") Long id,
            @Valid @RequestBody DeviceStatusChangeRequest request) {

        DeviceDetailResponse device = deviceService.changeDeviceStatus(id, request);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "장치 상태 변경 완료",
                device
        );
        return ResponseEntity.ok(response);
    }
}