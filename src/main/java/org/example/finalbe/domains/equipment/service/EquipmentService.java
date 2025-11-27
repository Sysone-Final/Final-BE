/**
 * 작성자: 황요한
 * 장비 서비스 클래스
 */
package org.example.finalbe.domains.equipment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.*;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companyserverroom.repository.CompanyServerRoomRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.dto.*;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.history.service.EquipmentHistoryRecorder;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.monitoring.service.ServerRoomDataSimulator;
import org.example.finalbe.domains.prometheus.service.EquipmentMappingService;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final MemberRepository memberRepository;
    private final CompanyServerRoomRepository companyServerRoomRepository;
    private final EquipmentHistoryRecorder equipmentHistoryRecorder;
    private final ServerRoomDataSimulator serverRoomDataSimulator;
    private final EquipmentMappingService equipmentMappingService;

    /**
     * 장비 목록 조회 (페이지네이션 + 필터)
     */
    public EquipmentPageResponse getEquipmentsWithFilters(
            int page, int size, String keyword, EquipmentType type, EquipmentStatus status,
            Long serverRoomId, Boolean onlyUnassigned) {

        Member currentMember = getCurrentMember();
        log.info("Fetching equipments with filters - page: {}, size: {}, keyword: {}, type: {}, status: {}, serverRoomId: {}, onlyUnassigned: {}, user: {} (role: {}, company: {})",
                page, size, keyword, type, status, serverRoomId, onlyUnassigned,
                currentMember.getId(), currentMember.getRole(), currentMember.getCompany().getId());

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Equipment> equipmentPage;

        if (currentMember.getRole() == Role.ADMIN) {
            equipmentPage = equipmentRepository.searchEquipmentsWithFilters(
                    keyword, type, status, serverRoomId, onlyUnassigned, DelYN.N, pageable);
            log.info("Admin user - fetched {} equipments", equipmentPage.getTotalElements());
        } else {
            Long companyId = currentMember.getCompany().getId();
            equipmentPage = equipmentRepository.searchEquipmentsWithFiltersByCompany(
                    keyword, type, status, serverRoomId, onlyUnassigned, companyId, DelYN.N, pageable);
            log.info("Non-admin user - fetched {} equipments for company: {}",
                    equipmentPage.getTotalElements(), companyId);
        }

        Page<EquipmentListResponse> responsePage = equipmentPage.map(EquipmentListResponse::from);

        return EquipmentPageResponse.from(responsePage);
    }

    /**
     * 랙별 장비 목록 조회
     */
    public RackWithEquipmentsResponse getEquipmentsByRack(
            Long rackId, String status, String type, String sortBy) {

        log.info("Fetching equipments for rack: {}", rackId);

        if (rackId == null || rackId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        Rack rack = rackRepository.findActiveById(rackId)
                .orElseThrow(() -> new EntityNotFoundException("랙", rackId));

        List<Equipment> equipments = equipmentRepository.findByRackIdAndDelYn(rackId, DelYN.N);

        List<EquipmentListResponse> equipmentResponses = equipments.stream()
                .filter(eq -> status == null || eq.getStatus().name().equals(status))
                .filter(eq -> type == null || eq.getType().name().equals(type))
                .sorted((e1, e2) -> {
                    if ("name".equals(sortBy)) {
                        return e1.getName().compareTo(e2.getName());
                    } else if ("status".equals(sortBy)) {
                        return e1.getStatus().compareTo(e2.getStatus());
                    } else if ("unit".equals(sortBy)) {
                        return e1.getStartUnit().compareTo(e2.getStartUnit());
                    }
                    return 0;
                })
                .map(EquipmentListResponse::from)
                .toList();

        log.info("Found {} equipments for rack: {}", equipmentResponses.size(), rackId);

        return RackWithEquipmentsResponse.from(rack, equipmentResponses);
    }

    /**
     * 장비 상세 조회
     */
    public EquipmentDetailResponse getEquipmentById(Long id) {
        log.info("Fetching equipment with id: {}", id);

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("장비", id));

        if (equipment.getDelYn() == DelYN.Y) {
            throw new EntityNotFoundException("장비", id);
        }

        return EquipmentDetailResponse.from(equipment);
    }

    /**
     * 서버실별 장비 목록 조회
     */
    public List<EquipmentListResponse> getEquipmentsByServerRoom(
            Long serverRoomId, String status, String type) {

        log.info("Fetching equipments for serverroom: {}", serverRoomId);

        if (serverRoomId == null || serverRoomId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 서버실 ID입니다.");
        }

        List<Equipment> equipments = equipmentRepository.findByServerRoomIdAndDelYn(serverRoomId, DelYN.N);

        return equipments.stream()
                .filter(eq -> status == null || eq.getStatus().name().equals(status))
                .filter(eq -> type == null || eq.getType().name().equals(type))
                .map(EquipmentListResponse::from)
                .toList();
    }

    /**
     * 장비 검색
     */
    public List<EquipmentListResponse> searchEquipments(String keyword, String type, String status) {
        log.info("Searching equipments with keyword: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        Member currentMember = getCurrentMember();

        List<Equipment> equipments;

        if (currentMember.getRole() == Role.ADMIN) {
            equipments = equipmentRepository.searchByKeywordAndDelYn(keyword, DelYN.N);
        } else {
            Long companyId = currentMember.getCompany().getId();
            equipments = equipmentRepository.searchByKeywordAndCompanyIdAndDelYn(
                    keyword, companyId, DelYN.N);
        }

        return equipments.stream()
                .filter(eq -> type == null || eq.getType().name().equals(type))
                .filter(eq -> status == null || eq.getStatus().name().equals(status))
                .map(EquipmentListResponse::from)
                .toList();
    }

    /**
     * 장비 생성
     */
    @Transactional
    public EquipmentDetailResponse createEquipment(EquipmentCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Creating equipment by user: {}", currentMember.getId());

        validateWritePermission(currentMember);

        if (request.equipmentName() == null || request.equipmentName().trim().isEmpty()) {
            throw new IllegalArgumentException("장비명을 입력해주세요.");
        }

        Rack rack = null;
        if (request.rackId() != null && request.rackId() > 0) {
            rack = rackRepository.findActiveById(request.rackId())
                    .orElseThrow(() -> new EntityNotFoundException("랙", request.rackId()));

            if (currentMember.getRole() != Role.ADMIN) {
                Long serverRoomId = rack.getServerRoom().getId();
                Long companyId = currentMember.getCompany().getId();

                boolean hasAccess = companyServerRoomRepository.existsByCompanyIdAndServerRoomId(
                        companyId, serverRoomId);

                if (!hasAccess) {
                    throw new AccessDeniedException("해당 랙에 대한 접근 권한이 없습니다.");
                }
            }
        }

        if (request.equipmentCode() != null && !request.equipmentCode().trim().isEmpty()) {
            if (equipmentRepository.existsByCodeAndDelYn(request.equipmentCode(), DelYN.N)) {
                throw new DuplicateException("장비 코드", request.equipmentCode());
            }
        }

        Equipment equipment = request.toEntity(rack);

        equipment.setCompanyId(currentMember.getCompany().getId());

        Equipment savedEquipment = equipmentRepository.save(equipment);

        if (rack != null) {
            rack.placeEquipment(savedEquipment, request.startUnit(), request.unitSize());

            equipmentMappingService.addEquipmentMapping(savedEquipment);

            if (savedEquipment.getType() == EquipmentType.SERVER ||
                    savedEquipment.getType() == EquipmentType.STORAGE) {
                try {
                    serverRoomDataSimulator.addEquipment(savedEquipment);
                } catch (Exception e) {
                    log.error("⚠️ 시뮬레이터 등록 실패: {}", e.getMessage());
                }
            }

            log.info("✅ 장비가 랙에 배치되어 메트릭 수집이 시작됩니다. (Equipment ID: {}, Rack ID: {})",
                    savedEquipment.getId(), rack.getId());
        } else {
            log.info("⊘ 장비가 랙에 배치되지 않아 메트릭 수집이 시작되지 않습니다. (Equipment ID: {})",
                    savedEquipment.getId());
        }

        equipmentHistoryRecorder.recordCreate(savedEquipment, currentMember);

        log.info("Equipment created successfully with id: {} for company: {}",
                savedEquipment.getId(), savedEquipment.getCompanyId());
        return EquipmentDetailResponse.from(savedEquipment);
    }

    /**
     * 장비 수정
     */
    @Transactional
    public EquipmentDetailResponse updateEquipment(Long id, EquipmentUpdateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Updating equipment with id: {} by user: {}", id, currentMember.getId());

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        validateWritePermission(currentMember);
        validateEquipmentAccess(currentMember, id);

        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("장비", id));

        if (equipment.getDelYn() == DelYN.Y) {
            throw new EntityNotFoundException("장비", id);
        }

        if (request.equipmentCode() != null
                && !request.equipmentCode().trim().isEmpty()
                && !request.equipmentCode().equals(equipment.getCode())) {
            if (equipmentRepository.existsByCodeAndDelYn(request.equipmentCode(), DelYN.N)) {
                throw new DuplicateException("장비 코드", request.equipmentCode());
            }
        }

        Equipment oldEquipment = cloneEquipment(equipment);

        Long oldRackId = equipment.getRack() != null ? equipment.getRack().getId() : null;

        boolean isStatusChanged = false;
        String oldStatus = null;
        String newStatus = null;

        if (request.status() != null) {
            oldStatus = equipment.getStatus() != null ? equipment.getStatus().name() : null;
            newStatus = request.status();
            isStatusChanged = !Objects.equals(oldStatus, newStatus);
        }

        equipment.updateInfo(
                request.equipmentName(),
                request.equipmentCode(),
                request.equipmentType() != null ? EquipmentType.valueOf(request.equipmentType()) : equipment.getType(),
                request.modelName(),
                request.manufacturer(),
                request.serialNumber(),
                request.startUnit(),
                request.ipAddress(),
                request.macAddress(),
                request.os(),
                request.cpuSpec(),
                request.memorySpec(),
                request.diskSpec(),
                request.powerConsumption(),
                request.status() != null ? EquipmentStatus.valueOf(request.status()) : equipment.getStatus(),
                request.installationDate(),
                request.notes(),
                request.monitoringEnabled(),
                request.cpuThresholdWarning(),
                request.cpuThresholdCritical(),
                request.memoryThresholdWarning(),
                request.memoryThresholdCritical(),
                request.diskThresholdWarning(),
                request.diskThresholdCritical()
        );

        Equipment updatedEquipment = equipmentRepository.save(equipment);

        Long newRackId = updatedEquipment.getRack() != null ? updatedEquipment.getRack().getId() : null;
        boolean rackChanged = !Objects.equals(oldRackId, newRackId);

        if (rackChanged) {
            equipmentMappingService.updateEquipmentMapping(updatedEquipment);

            EquipmentType type = updatedEquipment.getType();
            if (type == EquipmentType.SERVER || type == EquipmentType.STORAGE) {
                if (newRackId != null) {
                    try {
                        serverRoomDataSimulator.addEquipment(updatedEquipment);
                        log.info("✅ 장비가 랙에 배치되어 메트릭 수집 시작 (Equipment ID: {}, Rack ID: {})",
                                id, newRackId);
                    } catch (Exception e) {
                        log.error("⚠️ 시뮬레이터 등록 실패: {}", e.getMessage());
                    }
                } else {
                    try {
                        serverRoomDataSimulator.removeEquipment(id);
                        log.info("⊘ 장비가 랙에서 제거되어 메트릭 수집 중단 (Equipment ID: {})", id);
                    } catch (Exception e) {
                        log.error("⚠️ 시뮬레이터 제거 실패: {}", e.getMessage());
                    }
                }
            }
        }

        if (isStatusChanged) {
            equipmentHistoryRecorder.recordStatusChange(
                    updatedEquipment, oldStatus, newStatus, currentMember);
        } else {
            equipmentHistoryRecorder.recordUpdate(oldEquipment, updatedEquipment, currentMember);
        }

        log.info("Equipment updated successfully with id: {}", id);
        return EquipmentDetailResponse.from(updatedEquipment);
    }

    /**
     * 장비 삭제
     */
    @Transactional
    public void deleteEquipment(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Deleting equipment with id: {} by user: {}", id, currentMember.getId());

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        validateDeletePermission(currentMember);
        validateEquipmentAccess(currentMember, id);

        Equipment equipment = equipmentRepository.findByIdWithRackAndServerRoom(id)
                .orElseThrow(() -> new EntityNotFoundException("장비", id));

        if (equipment.getDelYn() == DelYN.Y) {
            throw new EntityNotFoundException("장비", id);
        }

        Rack rack = equipment.getRack();
        if (rack != null) {
            rack.releaseUnits(equipment.getUnitSize());

            if (equipment.getPowerConsumption() != null) {
                rack.subtractPowerUsage(equipment.getPowerConsumption());
            }
        }

        equipmentMappingService.removeEquipmentMapping(id);
        log.info("✅ 장비 삭제로 메트릭 수집 중단 (Equipment ID: {})", id);

        equipment.softDelete();

        equipmentHistoryRecorder.recordDelete(equipment, currentMember);

        log.info("Equipment deleted successfully with id: {}", id);
    }

    /**
     * 장비 대량 삭제
     */
    @Transactional
    public void deleteMultipleEquipments(List<Long> ids) {
        Member currentMember = getCurrentMember();
        log.info("Deleting multiple equipments: {} by user: {}", ids, currentMember.getId());

        validateDeletePermission(currentMember);

        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("삭제할 장비 ID 목록이 비어있습니다.");
        }

        for (Long id : ids) {
            try {
                deleteEquipment(id);
            } catch (Exception e) {
                log.warn("Failed to delete equipment with id: {}, error: {}", id, e.getMessage());
            }
        }

        log.info("Multiple equipments deleted successfully");
    }

    /**
     * 장비 대량 상태 변경
     */
    @Transactional
    public EquipmentStatusBulkUpdateResponse updateMultipleEquipmentStatus(
            EquipmentStatusBulkUpdateRequest request) {

        Member currentMember = getCurrentMember();
        log.info("Bulk updating equipment status by user: {}, ids: {}, new status: {}",
                currentMember.getId(), request.ids(), request.status());

        validateWritePermission(currentMember);

        if (request.ids() == null || request.ids().isEmpty()) {
            throw new IllegalArgumentException("장비 ID 목록이 비어있습니다.");
        }

        if (request.status() == null || request.status().trim().isEmpty()) {
            throw new IllegalArgumentException("변경할 상태를 입력해주세요.");
        }

        EquipmentStatus newStatus;
        try {
            newStatus = EquipmentStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 상태 값입니다: " + request.status());
        }

        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (Long equipmentId : request.ids()) {
            try {
                validateEquipmentAccess(currentMember, equipmentId);

                Equipment equipment = equipmentRepository.findById(equipmentId)
                        .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

                if (equipment.getDelYn() == DelYN.Y) {
                    log.warn("장비 ID {}는 이미 삭제되었습니다.", equipmentId);
                    failedIds.add(equipmentId);
                    continue;
                }

                String oldStatus = equipment.getStatus() != null ? equipment.getStatus().name() : null;

                equipment.setStatus(newStatus);
                equipmentRepository.save(equipment);

                equipmentHistoryRecorder.recordStatusChange(
                        equipment, newStatus.name(), oldStatus, currentMember);

                successIds.add(equipmentId);
                log.info("장비 ID {} 상태 변경 완료: {} -> {}", equipmentId, oldStatus, newStatus);

            } catch (Exception e) {
                log.error("장비 ID {} 상태 변경 실패: {}", equipmentId, e.getMessage());
                failedIds.add(equipmentId);
            }
        }

        log.info("대량 상태 변경 완료 - 성공: {}, 실패: {}", successIds.size(), failedIds.size());

        return EquipmentStatusBulkUpdateResponse.builder()
                .totalRequested(request.ids().size())
                .successCount(successIds.size())
                .failureCount(failedIds.size())
                .successIds(successIds)
                .failedEquipments(
                        failedIds.stream()
                                .map(id -> EquipmentStatusBulkUpdateResponse.FailedEquipment.builder()
                                        .equipmentId(id)
                                        .equipmentName(null)
                                        .reason("상태 변경 실패")
                                        .build())
                                .toList()
                )
                .build();

    }

    /**
     * 현재 로그인한 사용자 조회
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        String userId = authentication.getName();
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("사용자 ID가 존재하지 않습니다.");
        }

        try {
            return memberRepository.findById(Long.parseLong(userId))
                    .filter(m -> m.getDelYn() == DelYN.N)
                    .orElseThrow(() -> new EntityNotFoundException("회원", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 확인
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("장비를 생성/수정할 권한이 없습니다.");
        }
    }

    /**
     * 삭제 권한 확인
     */
    private void validateDeletePermission(Member member) {
        if (member.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("장비를 삭제할 권한이 없습니다.");
        }
    }

    /**
     * 장비 접근 권한 검증
     */
    private void validateEquipmentAccess(Member member, Long equipmentId) {
        if (member.getRole() == Role.ADMIN) {
            return;
        }

        Equipment equipment = equipmentRepository.findActiveById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

        if (equipment.getRack() == null) {
            if (!Objects.equals(equipment.getCompanyId(), member.getCompany().getId())) {
                throw new AccessDeniedException("해당 장비에 대한 접근 권한이 없습니다.");
            }
            return;
        }

        Long serverRoomId = equipment.getRack().getServerRoom().getId();

        boolean hasAccess = companyServerRoomRepository
                .existsByCompanyIdAndServerRoomId(member.getCompany().getId(), serverRoomId);

        if (!hasAccess) {
            throw new AccessDeniedException("해당 장비에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * 장비 복제 (히스토리 기록용)
     */
    private Equipment cloneEquipment(Equipment original) {
        return Equipment.builder()
                .id(original.getId())
                .companyId(original.getCompanyId())
                .name(original.getName())
                .code(original.getCode())
                .type(original.getType())
                .startUnit(original.getStartUnit())
                .unitSize(original.getUnitSize())
                .positionType(original.getPositionType())
                .modelName(original.getModelName())
                .manufacturer(original.getManufacturer())
                .serialNumber(original.getSerialNumber())
                .ipAddress(original.getIpAddress())
                .macAddress(original.getMacAddress())
                .os(original.getOs())
                .cpuSpec(original.getCpuSpec())
                .memorySpec(original.getMemorySpec())
                .diskSpec(original.getDiskSpec())
                .powerConsumption(original.getPowerConsumption())
                .status(original.getStatus())
                .installationDate(original.getInstallationDate())
                .notes(original.getNotes())
                .monitoringEnabled(original.getMonitoringEnabled())
                .cpuThresholdWarning(original.getCpuThresholdWarning())
                .cpuThresholdCritical(original.getCpuThresholdCritical())
                .memoryThresholdWarning(original.getMemoryThresholdWarning())
                .memoryThresholdCritical(original.getMemoryThresholdCritical())
                .diskThresholdWarning(original.getDiskThresholdWarning())
                .diskThresholdCritical(original.getDiskThresholdCritical())
                .delYn(original.getDelYn())
                .rack(original.getRack())
                .build();
    }
}