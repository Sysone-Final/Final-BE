package org.example.finalbe.domains.device.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.DeviceStatus;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.device.domain.Device;
import org.example.finalbe.domains.device.domain.DeviceType;
import org.example.finalbe.domains.device.dto.*;
import org.example.finalbe.domains.device.repository.DeviceRepository;

import org.example.finalbe.domains.device.repository.DeviceTypeRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final DataCenterRepository dataCenterRepository;
    private final RackRepository rackRepository;
    private final MemberRepository memberRepository;
    private final CompanyDataCenterRepository companyDataCenterRepository;

    /**
     * 현재 인증된 사용자 조회
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        String userId = authentication.getName();

        if (userId == null || userId.equals("anonymousUser")) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        try {
            return memberRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 확인
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() == Role.VIEWER) {
            throw new AccessDeniedException("조회 권한만 있습니다. 수정 권한이 필요합니다.");
        }
    }

    /**
     * 장치 접근 권한 확인
     */
    private void validateDeviceAccess(Member member, Long deviceId) {
        if (member.getRole() == Role.ADMIN) {
            return;
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("장치", deviceId));

        Long datacenterId = device.getDatacenter().getId();
        Long companyId = member.getCompany().getId();

        boolean hasAccess = companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                companyId, datacenterId);

        if (!hasAccess) {
            throw new AccessDeniedException("해당 장치에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 전산실별 장치 목록 조회
     */
    public List<DeviceListResponse> getDevicesByDatacenter(Long datacenterId) {
        log.info("Fetching devices for datacenter: {}", datacenterId);

        List<Device> devices = deviceRepository.findByDatacenterIdOrderByPosition(
                datacenterId, DelYN.N);

        return devices.stream()
                .map(DeviceListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 장치 상세 조회
     */
    public DeviceDetailResponse getDeviceById(Long id) {
        log.info("Fetching device with id: {}", id);

        Device device = deviceRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("장치", id));

        return DeviceDetailResponse.from(device);
    }

    /**
     * 장치 생성
     */
    @Transactional
    public DeviceDetailResponse createDevice(DeviceCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Creating device by user: {}", currentMember.getId());

        validateWritePermission(currentMember);

        // 장치 코드 중복 체크
        if (request.deviceCode() != null && !request.deviceCode().trim().isEmpty()) {
            if (deviceRepository.existsByDeviceCodeAndDelYn(request.deviceCode(), DelYN.N)) {
                throw new DuplicateException("장치 코드", request.deviceCode());
            }
        }

        // DeviceType 조회
        DeviceType deviceType = deviceTypeRepository.findById(request.deviceTypeId())
                .orElseThrow(() -> new EntityNotFoundException("장치 타입", request.deviceTypeId()));

        // DataCenter 조회 및 권한 확인
        DataCenter datacenter = dataCenterRepository.findActiveById(request.datacenterId())
                .orElseThrow(() -> new EntityNotFoundException("전산실", request.datacenterId()));

        if (currentMember.getRole() != Role.ADMIN) {
            boolean hasAccess = companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                    currentMember.getCompany().getId(), request.datacenterId());

            if (!hasAccess) {
                throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
            }
        }

        // Rack 조회 (server 타입일 경우)
        Rack rack = null;
        if (request.rackId() != null) {
            rack = rackRepository.findActiveById(request.rackId())
                    .orElseThrow(() -> new EntityNotFoundException("랙", request.rackId()));
        }

        Device device = request.toEntity(deviceType, datacenter, rack, currentMember.getId());
        Device savedDevice = deviceRepository.save(device);

        log.info("Device created successfully with id: {}", savedDevice.getId());
        return DeviceDetailResponse.from(savedDevice);
    }

    /**
     * 장치 수정
     */
    @Transactional
    public DeviceDetailResponse updateDevice(Long id, DeviceUpdateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Updating device with id: {} by user: {}", id, currentMember.getId());

        validateWritePermission(currentMember);
        validateDeviceAccess(currentMember, id);

        Device device = deviceRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("장치", id));

        // 필드 업데이트
        if (request.deviceName() != null) {
            device.setDeviceName(request.deviceName());
        }
        if (request.gridY() != null) {
            device.setGridY(request.gridY());
        }
        if (request.gridX() != null) {
            device.setGridX(request.gridX());
        }
        if (request.gridZ() != null) {
            device.setGridZ(request.gridZ());
        }
        if (request.rotation() != null) {
            device.setRotation(request.rotation());
        }
        if (request.status() != null) {
            device.setStatus(DeviceStatus.valueOf(request.status()));
        }
        if (request.modelName() != null) {
            device.setModelName(request.modelName());
        }
        if (request.manufacturer() != null) {
            device.setManufacturer(request.manufacturer());
        }
        if (request.serialNumber() != null) {
            device.setSerialNumber(request.serialNumber());
        }
        if (request.purchaseDate() != null) {
            device.setPurchaseDate(request.purchaseDate());
        }
        if (request.warrantyEndDate() != null) {
            device.setWarrantyEndDate(request.warrantyEndDate());
        }
        if (request.notes() != null) {
            device.setNotes(request.notes());
        }
        if (request.rackId() != null) {
            Rack rack = rackRepository.findActiveById(request.rackId())
                    .orElseThrow(() -> new EntityNotFoundException("랙", request.rackId()));
            device.setRack(rack);
        }

        log.info("Device updated successfully");
        return DeviceDetailResponse.from(device);
    }

    /**
     * 장치 위치 업데이트
     */
    @Transactional
    public DeviceDetailResponse updateDevicePosition(Long id, DevicePositionUpdateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Updating device position for id: {} by user: {}", id, currentMember.getId());

        validateWritePermission(currentMember);
        validateDeviceAccess(currentMember, id);

        Device device = deviceRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("장치", id));

        device.updatePosition(
                request.gridY(),
                request.gridX(),
                request.gridZ() != null ? request.gridZ() : 0,
                request.rotation() != null ? request.rotation() : 0
        );

        log.info("Device position updated successfully");
        return DeviceDetailResponse.from(device);
    }

    /**
     * 장치 상태 변경
     */
    @Transactional
    public DeviceDetailResponse changeDeviceStatus(Long id, DeviceStatusChangeRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Changing device status for id: {} to {} by user: {}",
                id, request.status(), currentMember.getId());

        validateWritePermission(currentMember);
        validateDeviceAccess(currentMember, id);

        Device device = deviceRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("장치", id));

        device.changeStatus(DeviceStatus.valueOf(request.status()), request.reason());

        log.info("Device status changed successfully");
        return DeviceDetailResponse.from(device);
    }

    /**
     * 장치 삭제
     */
    @Transactional
    public void deleteDevice(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Deleting device with id: {} by user: {}", id, currentMember.getId());

        validateWritePermission(currentMember);
        validateDeviceAccess(currentMember, id);

        Device device = deviceRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("장치", id));

        device.softDelete();

        log.info("Device soft deleted successfully");
    }
}