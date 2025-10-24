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
            throw new IllegalStateException("사용자 ID를 찾을 수 없습니다.");
        }

        try {
            return memberRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 확인 (ADMIN, OPERATOR만 가능)
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("관리자 또는 운영자만 수정할 수 있습니다.");
        }
    }

    /**
     * 회사별 부서 목록 조회
     */
    public List<DepartmentListResponse> getDepartmentsByCompany(Long companyId) {
        Member currentMember = getCurrentMember();
        log.debug("Fetching departments for company: {}", companyId);

        // ADMIN이 아니면 자신의 회사 부서만 조회 가능
        if (currentMember.getRole() != Role.ADMIN && !currentMember.getCompany().getId().equals(companyId)) {
            throw new AccessDeniedException("해당 회사의 부서 목록을 조회할 권한이 없습니다.");
        }

        List<Department> departments = departmentRepository.findByCompanyIdAndDelYn(companyId, DelYN.N);

        return departments.stream()
                .map(DepartmentListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 부서 상세 조회
     */
    public DepartmentDetailResponse getDepartmentById(Long id) {
        log.debug("Fetching department details for id: {}", id);

        Department department = departmentRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("부서", id));

        return DepartmentDetailResponse.from(department);
    }

    /**
     * 부서 생성
     */
    @Transactional
    public DepartmentDetailResponse createDepartment(DepartmentCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Creating new department: {} by user: {}", request.departmentName(), currentMember.getId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 회사 조회
        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));

        // 부서 코드 중복 체크 (같은 회사 내)
        if (departmentRepository.existsByCompanyIdAndDepartmentCodeAndDelYn(
                request.companyId(), request.departmentCode(), DelYN.N)) {
            throw new DuplicateException("부서 코드", request.departmentCode());
        }

        // 부서 생성
        Department department = request.toEntity(company, currentMember.getUserName());
        Department savedDepartment = departmentRepository.save(department);

        log.info("Department created successfully with id: {}", savedDepartment.getId());
        return DepartmentDetailResponse.from(savedDepartment);
    }

    /**
     * 부서 수정
     */
    @Transactional
    public DepartmentDetailResponse updateDepartment(Long id, DepartmentUpdateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Updating department with id: {} by user: {}", id, currentMember.getId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        Department department = departmentRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("부서", id));

        // 부서 정보 업데이트
        department.updateInfo(
                request.departmentName(),
                request.description(),
                request.location(),
                request.phone(),
                request.email(),
                currentMember.getUserName()
        );

        log.info("Department updated successfully for id: {}", id);
        return DepartmentDetailResponse.from(department);
    }

    /**
     * 부서 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteDepartment(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Deleting department with id: {} by user: {}", id, currentMember.getId());

        // ADMIN만 가능
        if (currentMember.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("관리자만 삭제할 수 있습니다.");
        }

        Department department = departmentRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("부서", id));

        // 소속 회원이 있는지 확인
        Long memberCount = memberDepartmentRepository.countByDepartmentId(id);
        if (memberCount > 0) {
            throw new BusinessException("부서에 소속된 회원이 있어 삭제할 수 없습니다.");
        }

        // 담당 랙이 있는지 확인
        Long rackCount = rackDepartmentRepository.countByDepartmentId(id);
        if (rackCount > 0) {
            throw new BusinessException("부서가 담당하는 랙이 있어 삭제할 수 없습니다.");
        }

        // 소프트 삭제
        department.softDelete();

        log.info("Department deleted successfully for id: {}", id);
    }

    /**
     * 부서 검색 (같은 회사 내)
     */
    public List<DepartmentListResponse> searchDepartments(Long companyId, String keyword) {
        Member currentMember = getCurrentMember();
        log.debug("Searching departments with keyword: {}", keyword);

        // ADMIN이 아니면 자신의 회사 내에서만 검색 가능
        if (currentMember.getRole() != Role.ADMIN && !currentMember.getCompany().getId().equals(companyId)) {
            throw new AccessDeniedException("해당 회사의 부서를 검색할 권한이 없습니다.");
        }

        List<Department> departments = departmentRepository.searchByNameInCompany(companyId, keyword);

        return departments.stream()
                .map(DepartmentListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원-부서 매핑 생성
     */
    @Transactional
    public void addMemberToDepartment(MemberDepartmentCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Adding member {} to department {}", request.memberId(), request.departmentId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 회원 조회
        Member member = memberRepository.findActiveById(request.memberId())
                .orElseThrow(() -> new EntityNotFoundException("회원", request.memberId()));

        // 부서 조회
        Department department = departmentRepository.findActiveById(request.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("부서", request.departmentId()));

        // 중복 체크
        if (memberDepartmentRepository.existsByMemberIdAndDepartmentId(request.memberId(), request.departmentId())) {
            throw new BusinessException("이미 해당 부서에 소속된 회원입니다.");
        }

        // 주 부서 설정 시, 기존 주 부서를 부 부서로 변경
        if (Boolean.TRUE.equals(request.isPrimary())) {
            memberDepartmentRepository.findByMemberIdAndIsPrimary(request.memberId(), true)
                    .ifPresent(MemberDepartment::setSecondaryDepartment);
        }

        // 매핑 생성
        MemberDepartment memberDepartment = MemberDepartment.builder()
                .member(member)
                .department(department)
                .isPrimary(Boolean.TRUE.equals(request.isPrimary()))
                .position(request.position())
                .joinDate(request.joinDate() != null ? request.joinDate() : LocalDate.now())
                .createdBy(currentMember.getUserName())
                .build();

        memberDepartmentRepository.save(memberDepartment);

        // 부서 직원 수 증가
        department.incrementEmployeeCount();

        log.info("Member {} added to department {} successfully", request.memberId(), request.departmentId());
    }

    /**
     * 회원-부서 매핑 삭제
     */
    @Transactional
    public void removeMemberFromDepartment(Long memberId, Long departmentId) {
        Member currentMember = getCurrentMember();
        log.info("Removing member {} from department {}", memberId, departmentId);

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        MemberDepartment memberDepartment = memberDepartmentRepository.findByMemberIdAndDepartmentId(memberId, departmentId)
                .orElseThrow(() -> new EntityNotFoundException("회원-부서 매핑을 찾을 수 없습니다."));

        Department department = memberDepartment.getDepartment();

        memberDepartmentRepository.delete(memberDepartment);

        // 부서 직원 수 감소
        department.decrementEmployeeCount();

        log.info("Member {} removed from department {} successfully", memberId, departmentId);
    }

    /**
     * 랙-부서 매핑 생성
     */
    @Transactional
    public void addRackToDepartment(RackDepartmentCreateRequest request) {
        Member currentMember = getCurrentMember();
        log.info("Adding rack {} to department {}", request.rackId(), request.departmentId());

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        // 랙 조회
        Rack rack = rackRepository.findActiveById(request.rackId())
                .orElseThrow(() -> new EntityNotFoundException("랙", request.rackId()));

        // 부서 조회
        Department department = departmentRepository.findActiveById(request.departmentId())
                .orElseThrow(() -> new EntityNotFoundException("부서", request.departmentId()));

        // 중복 체크
        if (rackDepartmentRepository.existsByRackIdAndDepartmentId(request.rackId(), request.departmentId())) {
            throw new BusinessException("이미 해당 부서가 담당하는 랙입니다.");
        }

        // 주 담당 부서 설정 시, 기존 주 담당 부서를 부 담당 부서로 변경
        if (Boolean.TRUE.equals(request.isPrimary())) {
            rackDepartmentRepository.findByRackIdAndIsPrimary(request.rackId(), true)
                    .ifPresent(RackDepartment::setSecondaryDepartment);
        }

        // 매핑 생성
        RackDepartment rackDepartment = RackDepartment.builder()
                .rack(rack)
                .department(department)
                .isPrimary(Boolean.TRUE.equals(request.isPrimary()))
                .responsibility(request.responsibility())
                .assignedDate(request.assignedDate() != null ? request.assignedDate() : LocalDate.now())
                .createdBy(currentMember.getUserName())
                .build();

        rackDepartmentRepository.save(rackDepartment);

        log.info("Rack {} added to department {} successfully", request.rackId(), request.departmentId());
    }

    /**
     * 랙-부서 매핑 삭제
     */
    @Transactional
    public void removeRackFromDepartment(Long rackId, Long departmentId) {
        Member currentMember = getCurrentMember();
        log.info("Removing rack {} from department {}", rackId, departmentId);

        // 쓰기 권한 확인
        validateWritePermission(currentMember);

        RackDepartment rackDepartment = rackDepartmentRepository.findByRackIdAndDepartmentId(rackId, departmentId)
                .orElseThrow(() -> new EntityNotFoundException("랙-부서 매핑을 찾을 수 없습니다."));

        rackDepartmentRepository.delete(rackDepartment);

        log.info("Rack {} removed from department {} successfully", rackId, departmentId);
    }

    /**
     * 부서별 랙 목록 조회
     */
    public List<RackListResponse> getRacksByDepartment(Long departmentId) {
        log.debug("Fetching racks for department: {}", departmentId);

        List<Rack> racks = rackDepartmentRepository.findRacksByDepartmentId(departmentId);

        return racks.stream()
                .map(RackListResponse::from)
                .collect(Collectors.toList());
    }
}