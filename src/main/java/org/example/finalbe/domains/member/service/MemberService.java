// src/main/java/org/example/finalbe/domains/member/service/MemberService.java

package org.example.finalbe.domains.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.dto.*;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 현재 로그인한 사용자 조회
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
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
     * 같은 회사 소속 검증
     */
    private void validateSameCompany(Member currentMember, Member targetMember) {
        if (!currentMember.getCompany().getId().equals(targetMember.getCompany().getId())) {
            throw new AccessDeniedException("다른 회사의 회원 정보는 접근할 수 없습니다.");
        }
    }

    /**
     * 회원 정보 수정 권한 검증
     * - 본인의 정보는 누구나 수정 가능
     * - OPERATOR는 같은 회사의 다른 사람도 수정 가능
     * - ADMIN은 모든 회원 수정 가능
     */
    private void validateUpdatePermission(Member currentMember, Member targetMember) {
        // 본인이면 수정 가능
        if (currentMember.getId().equals(targetMember.getId())) {
            return;
        }

        // ADMIN은 모든 회원 수정 가능
        if (currentMember.getRole() == Role.ADMIN) {
            return;
        }

        // OPERATOR는 같은 회사의 다른 사람도 수정 가능
        if (currentMember.getRole() == Role.OPERATOR) {
            validateSameCompany(currentMember, targetMember);
            return;
        }

        // VIEWER는 본인 외 수정 불가
        throw new AccessDeniedException("회원 정보를 수정할 권한이 없습니다.");
    }

    /**
     * 권한 변경 권한 검증
     */
    private void validateRoleChangePermission(Member currentMember, Member targetMember, String newRole) {
        Role newRoleEnum = Role.valueOf(newRole);

        // 자기 자신의 권한은 변경할 수 없음
        if (currentMember.getId().equals(targetMember.getId())) {
            throw new IllegalArgumentException("자기 자신의 권한은 변경할 수 없습니다.");
        }

        // ADMIN은 모든 권한 변경 가능
        if (currentMember.getRole() == Role.ADMIN) {
            return;
        }

        // OPERATOR는 VIEWER를 OPERATOR로만 변경 가능
        if (currentMember.getRole() == Role.OPERATOR) {
            if (targetMember.getRole() != Role.VIEWER) {
                throw new AccessDeniedException("OPERATOR는 VIEWER 권한만 변경할 수 있습니다.");
            }
            if (newRoleEnum != Role.OPERATOR) {
                throw new AccessDeniedException("OPERATOR는 VIEWER를 OPERATOR로만 변경할 수 있습니다.");
            }
            return; // ← 이 return이 빠져있었습니다!
        }

        // VIEWER는 권한 변경 불가
        throw new AccessDeniedException("권한을 변경할 수 있는 권한이 없습니다.");
    }

    /**
     * 회사별 활성 회원 목록 조회 (delYn='N'인 회원만)
     */
    public List<MemberListResponse> getMembersByCompany(Long companyId) {
        log.info("Fetching members for company: {}", companyId);

        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        // delYn='N'인 활성 회원만 조회
        List<Member> members = memberRepository.findByCompanyIdAndDelYn(companyId, DelYN.N);

        log.info("Found {} active members for company: {}", members.size(), companyId);

        return members.stream()
                .map(MemberListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원 상세 조회
     */
    public MemberDetailResponse getMemberById(Long memberId) {
        Member currentMember = getCurrentMember();

        log.info("Fetching member {} by user {}", memberId, currentMember.getId());

        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID를 입력해주세요.");
        }

        Member targetMember = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자", memberId));

        // 같은 회사 검증
        validateSameCompany(currentMember, targetMember);

        return MemberDetailResponse.from(targetMember);
    }

    /**
     * 회원 정보 수정 (권한 변경 포함)
     * - 본인 또는 OPERATOR 이상이면 정보 수정 가능
     * - 권한 변경은 별도 검증 로직 적용
     */
    @Transactional
    public MemberDetailResponse updateMember(Long memberId, MemberUpdateRequest request) {
        Member currentMember = getCurrentMember();

        log.info("Updating member {} by user {}", memberId, currentMember.getId());

        // === 1단계: 입력값 검증 ===
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID를 입력해주세요.");
        }

        // === 2단계: 수정할 회원 조회 ===
        Member targetMember = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자", memberId));

        // === 3단계: 수정 권한 검증 ===
        validateUpdatePermission(currentMember, targetMember);

        // === 4단계: 아이디 중복 체크 ===
        if (request.userName() != null && !request.userName().equals(targetMember.getUserName())) {
            if (memberRepository.existsByUserName(request.userName())) {
                throw new DuplicateException("아이디", request.userName());
            }
        }

        // === 5단계: 이메일 중복 체크 ===
        if (request.email() != null && !request.email().equals(targetMember.getEmail())) {
            if (memberRepository.existsByEmail(request.email())) {
                throw new DuplicateException("이메일", request.email());
            }
        }

        // === 6단계: 회원 정보 수정 ===
        targetMember.updateInfo(
                request.userName(),
                request.email(),
                request.phone()
        );

        // 주소 수정
        targetMember.updateAddress(
                request.city(),
                request.street(),
                request.zipcode()
        );

        // 비밀번호 변경
        if (request.password() != null && !request.password().trim().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(request.password());
            targetMember.changePassword(encodedPassword);
            log.info("Password changed for member {}", memberId);
        }

        // === 7단계: 권한 변경 (요청에 role이 포함된 경우) ===
        if (request.role() != null && !request.role().trim().isEmpty()) {
            // 현재 권한과 다른 경우에만 변경
            if (!targetMember.getRole().name().equals(request.role())) {
                validateRoleChangePermission(currentMember, targetMember, request.role());
                targetMember.updateRole(Role.valueOf(request.role()));
                log.info("Role changed for member {} to {}", memberId, request.role());
            }
        }

        // === 8단계: JPA Dirty Checking으로 자동 UPDATE ===
        log.info("Member {} updated successfully", memberId);

        return MemberDetailResponse.from(targetMember);
    }

    /**
     * 회원 삭제 (Soft Delete)
     * - ADMIN만 삭제 가능
     */
    @Transactional
    public void deleteMember(Long memberId) {
        Member currentMember = getCurrentMember();

        log.info("Deleting member {} by user {}", memberId, currentMember.getId());

        // === 1단계: ADMIN 권한 확인 ===
        if (currentMember.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("회원 삭제는 ADMIN만 가능합니다.");
        }

        // === 2단계: 입력값 검증 ===
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID를 입력해주세요.");
        }

        // 자기 자신은 삭제 불가
        if (currentMember.getId().equals(memberId)) {
            throw new IllegalArgumentException("자기 자신은 삭제할 수 없습니다.");
        }

        // === 3단계: 삭제할 회원 조회 ===
        Member targetMember = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자", memberId));

        // === 4단계: 같은 회사 검증 ===
        validateSameCompany(currentMember, targetMember);

        // === 5단계: Soft Delete 실행 ===
        targetMember.softDelete();

        log.info("Member {} soft deleted successfully", memberId);
    }
}