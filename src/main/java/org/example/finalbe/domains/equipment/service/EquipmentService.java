package org.example.finalbe.domains.equipment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.common.enumdir.EquipmentType;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.*;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.dto.*;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.history.service.EquipmentHistoryRecorder;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 장비 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final MemberRepository memberRepository;
    private final CompanyDataCenterRepository companyDataCenterRepository;
    private final EquipmentHistoryRecorder equipmentHistoryRecorder;

    /**
     * 메인 조회: 페이지네이션 + 전체 필터
     * GET /api/equipments?page=0&size=10&keyword=&type=&status=&datacenterId=
     */
    public EquipmentPageResponse getEquipmentsWithFilters(
            int page, int size, String keyword, EquipmentType type, EquipmentStatus status, Long datacenterId) {

        log.info("Fetching equipments with filters - page: {}, size: {}, keyword: {}, type: {}, status: {}, datacenterId: {}",
                page, size, keyword, type, status, datacenterId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Equipment> equipmentPage = equipmentRepository.searchEquipmentsWithFilters(
                keyword, type, status, datacenterId, DelYN.N, pageable);

        Page<EquipmentListResponse> responsePage = equipmentPage.map(EquipmentListResponse::from);

        return EquipmentPageResponse.from(responsePage);
    }

    /**
     * 랙별 장비 목록 조회 (기존 유지)
     */
    public List<EquipmentListResponse> getEquipmentsByRack(
            Long rackId, String status, String type, String sortBy) {

        log.info("Fetching equipments for rack: {}", rackId);

        if (rackId == null || rackId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 랙 ID입니다.");
        }

        List<Equipment> equipments = equipmentRepository.findByRackIdAndDelYn(rackId, DelYN.N);

        return equipments.stream()
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
     * 전산실별 장비 목록 조회 (기존 유지)
     */
    public List<EquipmentListResponse> getEquipmentsByDatacenter(
            Long datacenterId, String status, String type) {

        log.info("Fetching equipments for datacenter: {}", datacenterId);

        if (datacenterId == null || datacenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        List<Equipment> equipments = equipmentRepository.findByDatacenterIdAndDelYn(datacenterId, DelYN.N);

        return equipments.stream()
                .filter(eq -> status == null || eq.getStatus().name().equals(status))
                .filter(eq -> type == null || eq.getType().name().equals(type))
                .map(EquipmentListResponse::from)
                .toList();
    }

    /**
     * 장비 검색 (기존 유지)
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
        if (request.rackId() == null) {
            throw new IllegalArgumentException("랙을 선택해주세요.");
        }
        if (request.startUnit() == null) {
            throw new IllegalArgumentException("시작 유닛을 입력해주세요.");
        }
        if (request.unitSize() == null || request.unitSize() <= 0) {
            throw new IllegalArgumentException("유닛 크기를 입력해주세요.");
        }

        Rack rack = rackRepository.findActiveById(request.rackId())
                .orElseThrow(() -> new EntityNotFoundException("랙", request.rackId()));

        if (currentMember.getRole() != Role.ADMIN) {
            Long datacenterId = rack.getDatacenter().getId();
            Long companyId = currentMember.getCompany().getId();

            boolean hasAccess = companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                    companyId, datacenterId);

            if (!hasAccess) {
                throw new AccessDeniedException("해당 랙에 대한 접근 권한이 없습니다.");
            }
        }

        if (request.equipmentCode() != null && !request.equipmentCode().trim().isEmpty()) {
            if (equipmentRepository.existsByCodeAndDelYn(request.equipmentCode(), DelYN.N)) {
                throw new DuplicateException("장비 코드", request.equipmentCode());
            }
        }

        Equipment equipment = request.toEntity(rack, currentMember.getId());
        Equipment savedEquipment = equipmentRepository.save(equipment);

        rack.placeEquipment(savedEquipment, request.startUnit(), request.unitSize());

        equipmentHistoryRecorder.recordCreate(savedEquipment, currentMember);

        log.info("Equipment created successfully with id: {}", savedEquipment.getId());
        return EquipmentDetailResponse.from(savedEquipment);
    }

    /**
     * 장비 수정 (개선 버전 - 상태/위치 변경 감지)
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

        // === 수정 전 상태 저장 ===
        Equipment oldEquipment = cloneEquipment(equipment);

        // === 상태 변경 감지 ===
        boolean isStatusChanged = false;
        String oldStatus = null;
        String newStatus = null;

        if (request.status() != null) {
            oldStatus = equipment.getStatus() != null ? equipment.getStatus().name() : "UNKNOWN";
            newStatus = request.status();
            isStatusChanged = !oldStatus.equals(newStatus);
        }

        // === 위치 변경 감지 (유닛 위치 변경) ===
        boolean isLocationChanged = false;
        String oldLocation = null;
        String newLocation = null;

        // startUnit만 변경되었는지 확인
        if (request.startUnit() != null && !request.startUnit().equals(equipment.getStartUnit())) {
            isLocationChanged = true;
            oldLocation = String.format("Rack: %s, StartUnit: %d, UnitSize: %d",
                    equipment.getRack().getRackName(),
                    equipment.getStartUnit(),
                    equipment.getUnitSize());
            newLocation = String.format("Rack: %s, StartUnit: %d, UnitSize: %d",
                    equipment.getRack().getRackName(),
                    request.startUnit(),
                    equipment.getUnitSize());  // ✅ 기존 unitSize 사용
        }

        Rack rack = equipment.getRack();
        BigDecimal oldPower = equipment.getPowerConsumption();
        BigDecimal oldWeight = equipment.getWeight();

        // === 장비 정보 업데이트 ===
        equipment.updateInfo(
                request.equipmentName(),
                request.equipmentCode(),
                request.equipmentType() != null ? EquipmentType.valueOf(request.equipmentType()) : null,
                request.modelName(),
                request.manufacturer(),
                request.serialNumber(),
                request.ipAddress(),
                request.macAddress(),
                request.os(),
                request.cpuSpec(),
                request.memorySpec(),
                request.diskSpec(),
                request.powerConsumption(),
                request.weight(),
                request.status() != null ? EquipmentStatus.valueOf(request.status()) : null,
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

        // === 랙 전력/무게 재계산 ===
        BigDecimal newPower = equipment.getPowerConsumption();
        BigDecimal newWeight = equipment.getWeight();

        if (oldPower != null && newPower != null && !oldPower.equals(newPower)) {
            rack.setCurrentPowerUsage(
                    rack.getCurrentPowerUsage()
                            .subtract(oldPower)
                            .add(newPower)
            );
        }
        if (oldWeight != null && newWeight != null && !oldWeight.equals(newWeight)) {
            rack.setCurrentWeight(
                    rack.getCurrentWeight()
                            .subtract(oldWeight)
                            .add(newWeight)
            );
        }

        // === 히스토리 기록 (우선순위: 상태변경 > 위치변경 > 일반수정) ===
        // 상태만 변경된 경우 상태변경 히스토리 기록
        if (isStatusChanged && !hasOtherChanges(oldEquipment, equipment, "status")) {
            equipmentHistoryRecorder.recordStatusChange(equipment, oldStatus, newStatus, currentMember);
        }
        // 위치만 변경된 경우 위치변경 히스토리 기록
        else if (isLocationChanged && !hasOtherChanges(oldEquipment, equipment, "location")) {
            equipmentHistoryRecorder.recordMove(equipment, oldLocation, newLocation, currentMember);
        }
        // 여러 필드가 변경된 경우 일반 수정 히스토리 기록 (상세 변경 내역 포함)
        else {
            equipmentHistoryRecorder.recordUpdate(oldEquipment, equipment, currentMember);
        }

        log.info("Equipment updated successfully with id: {}", id);
        return EquipmentDetailResponse.from(equipment);
    }


    /**
     * 장비 삭제 (단건)
     */
    @Transactional
    public void deleteEquipment(Long id ) {
        Member currentMember = getCurrentMember();
        log.info("Deleting equipment with id: {} by user: {}", id, currentMember.getId());

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 장비 ID입니다.");
        }

        validateDeletePermission(currentMember);
        validateEquipmentAccess(currentMember, id);

        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("장비", id));


        if (equipment.getDelYn() == DelYN.Y) {
            throw new EntityNotFoundException("장비", id);
        }

        Rack rack = equipment.getRack();
        rack.removeEquipment(equipment);

        equipment.softDelete();

        equipmentHistoryRecorder.recordDelete(equipment, currentMember);

        log.info("Equipment deleted successfully with id: {}", id);
    }

    /**
     * 장비 대량 삭제
     * DELETE /api/equipments with body {ids: [1,2,3]}
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

    // ========== Private 헬퍼 메서드 ==========

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

    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("장비를 생성/수정할 권한이 없습니다.");
        }
    }

    private void validateDeletePermission(Member member) {
        if (member.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("장비를 삭제할 권한이 없습니다.");
        }
    }

    private void validateEquipmentAccess(Member member, Long equipmentId) {
        if (member.getRole() == Role.ADMIN) {
            return;
        }

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

        if (equipment.getDelYn() == DelYN.Y) {
            throw new EntityNotFoundException("장비", equipmentId);
        }

        Long datacenterId = equipment.getRack().getDatacenter().getId();
        Long companyId = member.getCompany().getId();

        boolean hasAccess = companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                companyId, datacenterId);

        if (!hasAccess) {
            throw new AccessDeniedException("해당 장비에 대한 접근 권한이 없습니다.");
        }
    }

    private Equipment cloneEquipment(Equipment equipment) {
        return Equipment.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .code(equipment.getCode())
                .type(equipment.getType())
                .startUnit(equipment.getStartUnit())
                .unitSize(equipment.getUnitSize())
                .positionType(equipment.getPositionType())
                .modelName(equipment.getModelName())
                .manufacturer(equipment.getManufacturer())
                .serialNumber(equipment.getSerialNumber())
                .ipAddress(equipment.getIpAddress())
                .macAddress(equipment.getMacAddress())
                .os(equipment.getOs())
                .cpuSpec(equipment.getCpuSpec())
                .memorySpec(equipment.getMemorySpec())
                .diskSpec(equipment.getDiskSpec())
                .powerConsumption(equipment.getPowerConsumption())
                .weight(equipment.getWeight())
                .status(equipment.getStatus())
                .installationDate(equipment.getInstallationDate())
                .notes(equipment.getNotes())
                .managerId(equipment.getManagerId())
                .rack(equipment.getRack())
                .monitoringEnabled(equipment.getMonitoringEnabled())
                .cpuThresholdWarning(equipment.getCpuThresholdWarning())
                .cpuThresholdCritical(equipment.getCpuThresholdCritical())
                .memoryThresholdWarning(equipment.getMemoryThresholdWarning())
                .memoryThresholdCritical(equipment.getMemoryThresholdCritical())
                .diskThresholdWarning(equipment.getDiskThresholdWarning())
                .diskThresholdCritical(equipment.getDiskThresholdCritical())
                .build();
    }

    /**
     * 특정 필드 외에 다른 필드도 변경되었는지 확인
     * @param oldEquipment 이전 장비
     * @param newEquipment 새 장비
     * @param excludeField 제외할 필드 ("status" 또는 "location")
     * @return 다른 필드가 변경되었으면 true
     */
    private boolean hasOtherChanges(Equipment oldEquipment, Equipment newEquipment, String excludeField) {
        boolean hasChanges = false;

        if (!"name".equals(excludeField) && !Objects.equals(oldEquipment.getName(), newEquipment.getName())) {
            hasChanges = true;
        }
        if (!"code".equals(excludeField) && !Objects.equals(oldEquipment.getCode(), newEquipment.getCode())) {
            hasChanges = true;
        }
        if (!"type".equals(excludeField) && !Objects.equals(oldEquipment.getType(), newEquipment.getType())) {
            hasChanges = true;
        }
        if (!"modelName".equals(excludeField) && !Objects.equals(oldEquipment.getModelName(), newEquipment.getModelName())) {
            hasChanges = true;
        }
        if (!"status".equals(excludeField) && !Objects.equals(oldEquipment.getStatus(), newEquipment.getStatus())) {
            hasChanges = true;
        }
        if (!"location".equals(excludeField) && !Objects.equals(oldEquipment.getStartUnit(), newEquipment.getStartUnit())) {
            hasChanges = true;
        }

        // 기타 주요 필드들도 체크 가능

        return hasChanges;
    }

    /**
     * 장비 대량 상태 변경
     *
     * @param request 대량 상태 변경 요청 (장비 ID 목록 + 변경할 상태)
     * @return 변경 결과 (성공/실패 개수, 상세 정보)
     */
    @Transactional
    public EquipmentStatusBulkUpdateResponse updateMultipleEquipmentStatus(
            EquipmentStatusBulkUpdateRequest request) {

        Member currentMember = getCurrentMember();
        log.info("Bulk updating equipment status by user: {} for {} equipments",
                currentMember.getId(), request.ids().size());

        // === 권한 확인 ===
        validateWritePermission(currentMember);

        // === 상태 enum 검증 ===
        EquipmentStatus newStatus;
        try {
            newStatus = EquipmentStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 상태 값입니다: " + request.status());
        }

        // === 결과 추적용 변수 ===
        List<Long> successIds = new java.util.ArrayList<>();
        List<EquipmentStatusBulkUpdateResponse.FailedEquipment> failedEquipments =
                new java.util.ArrayList<>();

        // === 각 장비에 대해 상태 변경 시도 ===
        for (Long equipmentId : request.ids()) {
            try {
                // 1. 장비 존재 여부 확인
                Equipment equipment = equipmentRepository.findById(equipmentId)
                        .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

                // 2. 삭제된 장비 확인
                if (equipment.getDelYn() == DelYN.Y) {
                    failedEquipments.add(EquipmentStatusBulkUpdateResponse.FailedEquipment.builder()
                            .equipmentId(equipmentId)
                            .equipmentName(equipment.getName())
                            .reason("삭제된 장비입니다.")
                            .build());
                    continue;
                }

                // 3. 접근 권한 확인
                if (currentMember.getRole() != Role.ADMIN) {
                    Long datacenterId = equipment.getRack().getDatacenter().getId();
                    Long companyId = currentMember.getCompany().getId();

                    boolean hasAccess = companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                            companyId, datacenterId);

                    if (!hasAccess) {
                        failedEquipments.add(EquipmentStatusBulkUpdateResponse.FailedEquipment.builder()
                                .equipmentId(equipmentId)
                                .equipmentName(equipment.getName())
                                .reason("접근 권한이 없습니다.")
                                .build());
                        continue;
                    }
                }

                // 4. 이미 동일한 상태인 경우 스킵
                if (equipment.getStatus() == newStatus) {
                    failedEquipments.add(EquipmentStatusBulkUpdateResponse.FailedEquipment.builder()
                            .equipmentId(equipmentId)
                            .equipmentName(equipment.getName())
                            .reason("이미 " + newStatus.name() + " 상태입니다.")
                            .build());
                    continue;
                }

                // 5. 상태 변경 전 저장 (히스토리 기록용)
                EquipmentStatus oldStatus = equipment.getStatus();

                // 6. 상태 변경
                equipment.updateStatus(newStatus);

                // 7. 히스토리 기록
                equipmentHistoryRecorder.recordStatusChange(
                        equipment,
                        oldStatus.name(),
                        newStatus.name(),
                        currentMember
                );

                // 8. 성공 목록에 추가
                successIds.add(equipmentId);

                log.info("Equipment {} status changed: {} → {}",
                        equipmentId, oldStatus, newStatus);

            } catch (Exception e) {
                // 예상치 못한 오류 처리
                log.error("Failed to update equipment {} status: {}", equipmentId, e.getMessage());
                failedEquipments.add(EquipmentStatusBulkUpdateResponse.FailedEquipment.builder()
                        .equipmentId(equipmentId)
                        .equipmentName("알 수 없음")
                        .reason("오류: " + e.getMessage())
                        .build());
            }
        }

        // === 결과 반환 ===
        EquipmentStatusBulkUpdateResponse response = EquipmentStatusBulkUpdateResponse.builder()
                .totalRequested(request.ids().size())
                .successCount(successIds.size())
                .failureCount(failedEquipments.size())
                .successIds(successIds)
                .failedEquipments(failedEquipments)
                .build();

        log.info("Bulk status update completed: {} success, {} failed",
                response.successCount(), response.failureCount());

        return response;
    }

}