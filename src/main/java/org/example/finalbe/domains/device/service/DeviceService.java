/**
 * 작성자: 황요한
 * 장치 서비스 클래스
 */
package org.example.finalbe.domains.device.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.DeviceStatus;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;
import org.example.finalbe.domains.serverroom.repository.ServerRoomRepository;
import org.example.finalbe.domains.device.domain.Device;
import org.example.finalbe.domains.device.domain.DeviceType;
import org.example.finalbe.domains.device.dto.*;
import org.example.finalbe.domains.device.repository.DeviceRepository;
import org.example.finalbe.domains.device.repository.DeviceTypeRepository;
import org.example.finalbe.domains.history.service.DeviceHistoryRecorder;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final ServerRoomRepository serverRoomRepository;
    private final RackRepository rackRepository;
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository companyServerRoomRepository;
    private final DeviceHistoryRecorder deviceHistoryRecorder;
    private final EquipmentRepository equipmentRepository;

    /**
     * 서버실별 장치 목록 조회
     */
    public ServerRoomDeviceListResponse getDevicesByServerRoom(Long serverRoomId) {
        log.info("Fetching devices for serverroom: {}", serverRoomId);

        ServerRoom serverRoom = serverRoomRepository.findActiveById(serverRoomId)
                .orElseThrow(() -> new EntityNotFoundException("서버실", serverRoomId));

        List<Device> devices = deviceRepository.findByServerRoomIdOrderByPosition(
                serverRoomId, DelYN.N);

        List<Equipment> equipments = equipmentRepository.findByServerRoomIdAndDelYn(
                serverRoomId, DelYN.N);
        Integer totalEquipmentCount = equipments.size();

        List<Long> rackIds = devices.stream()
                .filter(device -> device.getRack() != null)
                .map(device -> device.getRack().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Long> equipmentCountMap = new HashMap<>();
        if (!rackIds.isEmpty()) {
            List<EquipmentRepository.RackEquipmentCount> counts =
                    equipmentRepository.countEquipmentsByRackIds(rackIds, DelYN.N);

            for (EquipmentRepository.RackEquipmentCount count : counts) {
                equipmentCountMap.put(count.getRackId(), count.getCount());
            }
        }

        ServerRoomInfo serverRoomInfo = ServerRoomInfo.from(serverRoom);
        List<DeviceSimpleInfo> deviceInfos = devices.stream()
                .map(device -> {
                    if (device.getRack() != null) {
                        Long rackId = device.getRack().getId();
                        Integer equipmentCount = equipmentCountMap
                                .getOrDefault(rackId, 0L)
                                .intValue();
                        return DeviceSimpleInfo.from(device, equipmentCount);
                    } else {
                        return DeviceSimpleInfo.from(device, 0);
                    }
                })
                .collect(Collectors.toList());

        return ServerRoomDeviceListResponse.of(serverRoomInfo, deviceInfos, totalEquipmentCount);
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

        if (request.deviceCode() != null && !request.deviceCode().trim().isEmpty()) {
            if (deviceRepository.existsByDeviceCodeAndDelYn(request.deviceCode(), DelYN.N)) {
                throw new DuplicateException("장치 코드", request.deviceCode());
            }
        }

        DeviceType deviceType = deviceTypeRepository.findById(request.deviceTypeId())
                .orElseThrow(() -> new EntityNotFoundException("장치 타입", request.deviceTypeId()));

        ServerRoom serverRoom = serverRoomRepository.findActiveById(request.serverRoomId())
                .orElseThrow(() -> new EntityNotFoundException("서버실", request.serverRoomId()));

        if (currentMember.getRole() != Role.ADMIN) {
            boolean hasAccess = companyServerRoomRepository.existsByCompanyIdAndServerRoomId(
                    currentMember.getCompany().getId(), request.serverRoomId());

            if (!hasAccess) {
                throw new AccessDeniedException("해당 서버실에 대한 접근 권한이 없습니다.");
            }
        }

        Rack rack = null;
        if (request.rackId() != null) {
            rack = rackRepository.findActiveById(request.rackId())
                    .orElseThrow(() -> new EntityNotFoundException("랙", request.rackId()));

            if (deviceRepository.existsActiveDeviceByRackId(request.rackId())) {
                throw new BusinessException("이미 다른 장치가 해당 랙에 배치되어 있습니다. 한 랙에는 하나의 장치만 배치할 수 있습니다.");
            }
        }

        Device device = request.toEntity(deviceType, serverRoom, rack);
        Device savedDevice = deviceRepository.save(device);

        deviceHistoryRecorder.recordCreate(savedDevice, currentMember);

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

        Device oldDevice = cloneDevice(device);

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

            if (device.getRack() == null || !device.getRack().getId().equals(request.rackId())) {
                if (deviceRepository.existsActiveDeviceByRackIdExcludingDevice(request.rackId(), id)) {
                    throw new BusinessException("이미 다른 장치가 해당 랙에 배치되어 있습니다. 한 랙에는 하나의 장치만 배치할 수 있습니다.");
                }
            }

            device.setRack(rack);
        }

        deviceHistoryRecorder.recordUpdate(oldDevice, device, currentMember);

        log.info("Device updated successfully");
        return DeviceDetailResponse.from(device);
    }

    /**
     * 장치 위치 변경
     */
    @Transactional
    public DeviceDetailResponse updateDevicePosition(Long id, DevicePositionUpdateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Updating device position for id: {} by user: {}", id, currentMember.getId());

        validateWritePermission(currentMember);
        validateDeviceAccess(currentMember, id);

        Device device = deviceRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("장치", id));

        String oldPosition = String.format("(%d, %d, %d) rotation: %d",
                device.getGridY(), device.getGridX(), device.getGridZ(), device.getRotation());

        device.updatePosition(
                request.gridY(),
                request.gridX(),
                request.gridZ() != null ? request.gridZ() : 0,
                request.rotation() != null ? request.rotation() : 0
        );

        String newPosition = String.format("(%d, %d, %d) rotation: %d",
                device.getGridY(), device.getGridX(), device.getGridZ(), device.getRotation());

        deviceHistoryRecorder.recordMove(device, oldPosition, newPosition, currentMember);

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

        String oldStatus = device.getStatus() != null ? device.getStatus().name() : "UNKNOWN";

        device.changeStatus(DeviceStatus.valueOf(request.status()), request.reason());

        deviceHistoryRecorder.recordStatusChange(device, oldStatus, request.status(), currentMember);

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

        deviceHistoryRecorder.recordDelete(device, currentMember);

        log.info("Device soft deleted successfully");
    }

    /**
     * 현재 로그인한 사용자 조회
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

        Long serverRoomId = device.getServerRoom().getId();
        Long companyId = member.getCompany().getId();

        boolean hasAccess = companyServerRoomRepository.existsByCompanyIdAndServerRoomId(
                companyId, serverRoomId);

        if (!hasAccess) {
            throw new AccessDeniedException("해당 장치에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 장치 복제 (히스토리 기록용)
     */
    private Device cloneDevice(Device device) {
        return Device.builder()
                .id(device.getId())
                .deviceName(device.getDeviceName())
                .deviceCode(device.getDeviceCode())
                .gridY(device.getGridY())
                .gridX(device.getGridX())
                .gridZ(device.getGridZ())
                .rotation(device.getRotation())
                .status(device.getStatus())
                .modelName(device.getModelName())
                .manufacturer(device.getManufacturer())
                .serialNumber(device.getSerialNumber())
                .purchaseDate(device.getPurchaseDate())
                .warrantyEndDate(device.getWarrantyEndDate())
                .notes(device.getNotes())
                .deviceType(device.getDeviceType())
                .serverRoom(device.getServerRoom())
                .rack(device.getRack())
                .build();
    }
}