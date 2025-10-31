package org.example.finalbe.domains.device.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.device.domain.DeviceType;
import org.example.finalbe.domains.device.dto.DeviceTypeListResponse;
import org.example.finalbe.domains.device.repository.DeviceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 장치 타입 서비스
 * 장치 타입 조회 처리
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTypeService {

    private final DeviceTypeRepository deviceTypeRepository;

    /**
     * 모든 장치 타입 조회
     */
    public List<DeviceTypeListResponse> getAllDeviceTypes() {
        log.info("Fetching all device types");

        List<DeviceType> deviceTypes = deviceTypeRepository.findAll();

        return deviceTypes.stream()
                .map(DeviceTypeListResponse::from)
                .collect(Collectors.toList());
    }
}