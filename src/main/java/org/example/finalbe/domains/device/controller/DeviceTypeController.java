package org.example.finalbe.domains.device.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.example.finalbe.domains.device.dto.DeviceTypeListResponse;
import org.example.finalbe.domains.device.service.DeviceTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 장치 타입 컨트롤러
 * 장치 타입 조회 API 제공
 */
@RestController
@RequestMapping("/api/device-types")
@RequiredArgsConstructor
@Validated
public class DeviceTypeController {

    private final DeviceTypeService deviceTypeService;

    /**
     * 모든 장치 타입 조회
     * GET /api/device-types
     *
     * @return 장치 타입 목록
     */
    @GetMapping
    public ResponseEntity<CommonResDto> getAllDeviceTypes() {
        List<DeviceTypeListResponse> deviceTypes = deviceTypeService.getAllDeviceTypes();
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "장치 타입 목록 조회 완료", deviceTypes));
    }
}