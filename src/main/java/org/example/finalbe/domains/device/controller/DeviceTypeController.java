package org.example.finalbe.domains.device.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;

import org.example.finalbe.domains.device.dto.DeviceTypeCreateRequest;
import org.example.finalbe.domains.device.dto.DeviceTypeListResponse;
import org.example.finalbe.domains.device.service.DeviceTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 장치 타입 컨트롤러
 * server, door, climatic_chamber, fire_extinguisher, thermometer, aircon 등의 타입 관리
 */
@RestController
@RequestMapping("/api/device-types")
@RequiredArgsConstructor
@Validated
public class DeviceTypeController {

    private final DeviceTypeService deviceTypeService;

    /**
     * 모든 장치 타입 조회
     * 시스템에서 사용 가능한 모든 장치 타입 목록 반환
     * 권한: 모든 인증된 사용자 접근 가능
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAllDeviceTypes() {
        List<DeviceTypeListResponse> deviceTypes = deviceTypeService.getAllDeviceTypes();
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "장치 타입 목록 조회 완료",
                deviceTypes
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 새로운 장치 타입 생성
     * 시스템에 새로운 장치 타입 추가
     * 권한: ADMIN만 가능
     *
     * @param request 장치 타입 생성 요청 (Validation 적용)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> createDeviceType(@Valid @RequestBody DeviceTypeCreateRequest request) {
        DeviceTypeListResponse deviceType = deviceTypeService.createDeviceType(request);
        CommonResDto response = new CommonResDto(
                HttpStatus.CREATED,
                "장치 타입 생성 완료",
                deviceType
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 장치 타입 삭제
     * 시스템에서 장치 타입 제거
     * 주의: 해당 타입을 사용하는 장치가 있으면 삭제 불가
     * 권한: ADMIN만 가능
     *
     * @param id 장치 타입 ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResDto> deleteDeviceType(
            @PathVariable @Min(value = 1, message = "유효하지 않은 장치 타입 ID입니다.") Long id) {

        deviceTypeService.deleteDeviceType(id);
        CommonResDto response = new CommonResDto(
                HttpStatus.OK,
                "장치 타입 삭제 완료",
                null
        );
        return ResponseEntity.ok(response);
    }
}