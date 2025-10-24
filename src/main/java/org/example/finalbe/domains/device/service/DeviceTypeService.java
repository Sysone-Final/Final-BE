package org.example.finalbe.domains.device.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;

import org.example.finalbe.domains.device.domain.DeviceType;
import org.example.finalbe.domains.device.dto.DeviceTypeCreateRequest;
import org.example.finalbe.domains.device.dto.DeviceTypeListResponse;
import org.example.finalbe.domains.device.repository.DeviceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
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

    /**
     * 장치 타입 생성
     */
    @Transactional
    public DeviceTypeListResponse createDeviceType(DeviceTypeCreateRequest request) {
        log.info("Creating device type: {}", request.typeName());

        // 중복 체크
        if (deviceTypeRepository.existsByTypeName(request.typeName())) {
            throw new DuplicateException("장치 타입", request.typeName());
        }

        DeviceType deviceType = request.toEntity();
        DeviceType savedDeviceType = deviceTypeRepository.save(deviceType);

        log.info("Device type created successfully with id: {}", savedDeviceType.getId());
        return DeviceTypeListResponse.from(savedDeviceType);
    }

    /**
     * 장치 타입 삭제
     */
    @Transactional
    public void deleteDeviceType(Long id) {
        log.info("Deleting device type with id: {}", id);

        DeviceType deviceType = deviceTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("장치 타입", id));

        deviceTypeRepository.delete(deviceType);
        log.info("Device type deleted successfully");
    }
}