package org.example.finalbe.domains.department.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.BusinessException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.department.domain.Department;
import org.example.finalbe.domains.department.domain.MemberDepartment;
import org.example.finalbe.domains.department.domain.RackDepartment;
import org.example.finalbe.domains.department.dto.*;
import org.example.finalbe.domains.department.repository.DepartmentRepository;
import org.example.finalbe.domains.department.repository.MemberDepartmentRepository;
import org.example.finalbe.domains.department.repository.RackDepartmentRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.dto.RackListResponse;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Department 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final MemberDepartmentRepository memberDepartmentRepository;
    private final RackDepartmentRepository rackDepartmentRepository;
    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final RackRepository rackRepository;

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
            return memberRepository.findActiveById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 검증
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() == Role.VIEWER) {
            throw new AccessDeniedException("조회 권한만 있습니다. 수정 권한이 필요합니다.");
        }
    }

    /**
     * 부서 생성
     */
    @Transactional
    public DepartmentDetailResponse createDepartment(DepartmentCreateRequest request) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));

        if (departmentRepository.existsByCompanyIdAndDepartmentCodeAndDelYn(
                request.companyId(), request.departmentCode(), DelYN.N)) {
            throw new DuplicateException("부서 코드", request.departmentCode());
        }

        Department department = request.toEntity(company, currentMember.getUserName());
        Department savedDepartment = departmentRepository.save(department);

        log.info("부서 생성 완료: {}", savedDepartment.getDepartmentName());
        return DepartmentDetailResponse.from(savedDepartment);
    }

    /**
     * 부서 목록 조회 (회사별)
     */
    public List<DepartmentListResponse> getDepartmentsByCompany(Long companyId) {
        Member currentMember = getCurrentMember();

        List<Department> departments = departmentRepository.findByCompanyIdAndDelYn(companyId, DelYN.N);
        return departments.stream()
                .map(DepartmentListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 부서 상세 조회
     */
    public DepartmentDetailResponse getDepartmentById(Long departmentId) {
        getCurrentMember();

        Department department = departmentRepository.findActiveById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("부서", departmentId));

        return DepartmentDetailResponse.from(department);
    }

    /**
     * 부서 수정
     */
    @Transactional
    public DepartmentDetailResponse updateDepartment(Long departmentId, DepartmentUpdateRequest request) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        Department department = departmentRepository.findActiveById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("부서", departmentId));

        department.updateInfo(
                request.departmentName(),
                request.description(),
                request.location(),
                request.phone(),
                request.email(),
                currentMember.getUserName()
        );

        log.info("부서 수정 완료: {}", department.getDepartmentName());
        return DepartmentDetailResponse.from(department);
    }

    /**
     * 부서 삭제
     */
    @Transactional
    public void deleteDepartment(Long departmentId) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        Department department = departmentRepository.findActiveById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("부서", departmentId));

        Long memberCount = memberDepartmentRepository.countByDepartmentId(departmentId);
        if (memberCount > 0) {
            throw new BusinessException("소속 직원이 있는 부서는 삭제할 수 없습니다.");
        }

        department.softDelete();
        log.info("부서 삭제 완료: {}", department.getDepartmentName());
    }

    /**
     * 회원-부서 매핑 생성
     */
    @Transactional
    public void assignMemberToDepartment(MemberDepartmentCreateRequest request) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        if (memberDepartmentRepository.existsByMemberIdAndDepartmentId(
                request.memberId(), request.departmentId())) {
            throw new DuplicateException("회원-부서 매핑", "이미 해당 부서에 배치되어 있습니다.");
        }

        Member member = memberRepository.findActiveById(request.memberId())
                .orElseThrow(() -> new EntityNotFoundException("회원", request.memberId()));

        Department department = departmentRepository.findActiveById(request.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("부서", request.departmentId()));

        boolean isPrimary = request.isPrimary() != null && request.isPrimary();

        if (isPrimary) {
            memberDepartmentRepository.findByMemberIdAndIsPrimary(request.memberId(), true)
                    .ifPresent(md -> md.setSecondaryDepartment());
        }

        MemberDepartment memberDepartment = MemberDepartment.builder()
                .member(member)
                .department(department)
                .isPrimary(isPrimary)
                .position(request.position())
                .joinDate(request.joinDate() != null ? request.joinDate() : LocalDate.now())
                .createdBy(currentMember.getUserName())
                .build();

        memberDepartmentRepository.save(memberDepartment);
        department.incrementEmployeeCount();

        log.info("회원-부서 매핑 생성: 회원={}, 부서={}", member.getName(), department.getDepartmentName());
    }

    /**
     * 회원-부서 매핑 삭제
     */
    @Transactional
    public void removeMemberFromDepartment(Long memberId, Long departmentId) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        MemberDepartment memberDepartment = memberDepartmentRepository
                .findByMemberIdAndDepartmentId(memberId, departmentId)
                .orElseThrow(() -> new EntityNotFoundException("회원-부서 매핑을 찾을 수 없습니다."));

        Department department = memberDepartment.getDepartment();
        memberDepartmentRepository.delete(memberDepartment);
        department.decrementEmployeeCount();

        log.info("회원-부서 매핑 삭제: 회원 ID={}, 부서={}", memberId, department.getDepartmentName());
    }

    /**
     * 랙-부서 매핑 생성
     */
    @Transactional
    public void assignRackToDepartment(RackDepartmentCreateRequest request) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        if (rackDepartmentRepository.existsByRackIdAndDepartmentId(
                request.rackId(), request.departmentId())) {
            throw new DuplicateException("랙-부서 매핑", "이미 해당 부서에 배정되어 있습니다.");
        }

        Rack rack = rackRepository.findActiveById(request.rackId())
                .orElseThrow(() -> new EntityNotFoundException("랙", request.rackId()));

        Department department = departmentRepository.findActiveById(request.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("부서", request.departmentId()));

        boolean isPrimary = request.isPrimary() != null && request.isPrimary();

        if (isPrimary) {
            rackDepartmentRepository.findByRackIdAndIsPrimary(request.rackId(), true)
                    .ifPresent(rd -> rd.setSecondaryDepartment());
        }

        RackDepartment rackDepartment = RackDepartment.builder()
                .rack(rack)
                .department(department)
                .isPrimary(isPrimary)
                .responsibility(request.responsibility())
                .assignedDate(request.assignedDate() != null ? request.assignedDate() : LocalDate.now())
                .createdBy(currentMember.getUserName())
                .build();

        rackDepartmentRepository.save(rackDepartment);
        log.info("랙-부서 매핑 생성: 랙={}, 부서={}", rack.getRackName(), department.getDepartmentName());
    }

    /**
     * 랙-부서 매핑 삭제
     */
    @Transactional
    public void removeRackFromDepartment(Long rackId, Long departmentId) {
        Member currentMember = getCurrentMember();
        validateWritePermission(currentMember);

        RackDepartment rackDepartment = rackDepartmentRepository
                .findByRackIdAndDepartmentId(rackId, departmentId)
                .orElseThrow(() -> new EntityNotFoundException("랙-부서 매핑을 찾을 수 없습니다."));

        rackDepartmentRepository.delete(rackDepartment);
        log.info("랙-부서 매핑 삭제: 랙 ID={}, 부서={}", rackId, rackDepartment.getDepartment().getDepartmentName());
    }

    /**
     * 부서별 랙 목록 조회
     */
    public List<RackListResponse> getRacksByDepartment(Long departmentId) {
        getCurrentMember();

        departmentRepository.findActiveById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("부서", departmentId));

        List<Rack> racks = rackDepartmentRepository.findRacksByDepartmentId(departmentId);
        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }
}