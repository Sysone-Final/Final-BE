package org.example.finalbe.domains.equipment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.EquipmentStatus;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.*;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.dto.*;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final RackRepository rackRepository;
    private final MemberRepository memberRepository;
    private final CompanyDataCenterRepository companyDataCenterRepository;

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

    private void validateWritePermission(Member member) {
        if (member.getRole() == Role.VIEWER) {
            throw new AccessDeniedException("조회 권한만 있습니다. 수정 권한이 필요합니다.");
        }
    }

    private void validateEquipmentAccess(Member member, Long equipmentId) {
        if (member.getRole() == Role.ADMIN) {
            return;
        }

        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new EntityNotFoundException("장비", equipmentId));

        Long datacenterId = equipment.getRack().getDatacenter().getId();
        Long companyId = member.getCompany().getId();

        // CompanyDataCenter 테이블에서 접근 권한 확인
        boolean hasAccess = companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                companyId, datacenterId);

        if (!hasAccess) {
            throw new AccessDeniedException("해당 장비에 대한 접근 권한이 없습니다.");
        }
    }


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
                .collect(Collectors.toList());
    }

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

        // ✅ 수정된 부분 - CompanyDataCenter 테이블에서 접근 권한 확인
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

        log.info("Equipment created successfully with id: {}", savedEquipment.getId());
        return EquipmentDetailResponse.from(savedEquipment);
    }

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

        Rack rack = equipment.getRack();
        BigDecimal oldPower = equipment.getPowerConsumption();
        BigDecimal oldWeight = equipment.getWeight();

        equipment.updateInfo(request);

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

        log.info("Equipment updated successfully with id: {}", id);
        return EquipmentDetailResponse.from(equipment);
    }

    @Transactional
    public void deleteEquipment(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Deleting equipment with id: {} by user: {}", id, currentMember.getId());

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

        Rack rack = equipment.getRack();
        rack.removeEquipment(equipment);

        equipment.softDelete();

        log.info("Equipment soft deleted successfully with id: {}", id);
    }

    @Transactional
    public EquipmentDetailResponse changeEquipmentStatus(Long id, EquipmentStatusChangeRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Changing equipment status for id: {} to {} by user: {}",
                id, request.status(), currentMember.getId());

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

        equipment.changeStatus(
                EquipmentStatus.valueOf(request.status()),
                request.reason(),
                currentMember.getUserName()
        );

        log.info("Equipment status changed successfully");
        return EquipmentDetailResponse.from(equipment);
    }

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
                .collect(Collectors.toList());
    }

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
                .collect(Collectors.toList());
    }
}