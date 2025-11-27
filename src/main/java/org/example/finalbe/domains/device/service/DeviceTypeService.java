// 작성자: 황요한
// 설명: DeviceType 관련 조회 기능을 제공하는 서비스 클래스

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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceTypeService {

    private final DeviceTypeRepository deviceTypeRepository;

    /**
     * 모든 장치 타입을 조회하여 DTO 리스트로 반환
     */
    public List<DeviceTypeListResponse> getAllDeviceTypes() {
        log.info("Fetching all device types");
        return deviceTypeRepository.findAll().stream()
                .map(DeviceTypeListResponse::from)
                .collect(Collectors.toList());
    }
}
