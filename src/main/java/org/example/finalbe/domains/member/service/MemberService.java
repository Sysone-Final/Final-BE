// 작성자: 황요한
// 회원 서비스: 회원 조회, 수정, 삭제 처리

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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /** 현재 로그인한 회원 조회 */
    private Member getCurrentMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        String userId = auth.getName();
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

    /** 같은 회사인지 검증 */
    private void validateSameCompany(Member current, Member target) {
        if (!current.getCompany().getId().equals(target.getCompany().getId())) {
            throw new AccessDeniedException("다른 회사의 회원 정보는 접근할 수 없습니다.");
        }
    }

    /** 수정 권한 검증 */
    private void validateUpdatePermission(Member current, Member target) {
        if (current.getId().equals(target.getId())) return;
        if (current.getRole() == Role.ADMIN) return;
        if (current.getRole() == Role.OPERATOR) {
            validateSameCompany(current, target);
            return;
        }
        throw new AccessDeniedException("회원 정보를 수정할 권한이 없습니다.");
    }

    /** 권한 변경 가능 여부 검증 */
    private void validateRoleChangePermission(Member current, Member target, String newRole) {
        Role newRoleEnum = Role.valueOf(newRole);

        if (current.getId().equals(target.getId())) {
            throw new IllegalArgumentException("자기 자신의 권한은 변경할 수 없습니다.");
        }
        if (current.getRole() == Role.ADMIN) return;

        if (current.getRole() == Role.OPERATOR) {
            if (target.getRole() != Role.VIEWER) {
                throw new AccessDeniedException("OPERATOR는 VIEWER 권한만 변경할 수 있습니다.");
            }
            if (newRoleEnum != Role.OPERATOR) {
                throw new AccessDeniedException("OPERATOR는 VIEWER를 OPERATOR로만 변경할 수 있습니다.");
            }
            return;
        }

        throw new AccessDeniedException("권한을 변경할 수 없습니다.");
    }

    /** 회사별 회원 목록 조회 */
    public List<MemberListResponse> getMembersByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("회사 ID를 입력해주세요.");
        }

        List<Member> members = memberRepository.findByCompanyIdAndDelYn(companyId, DelYN.N);

        return members.stream()
                .map(MemberListResponse::from)
                .collect(Collectors.toList());
    }

    /** 회원 상세 조회 */
    public MemberDetailResponse getMemberById(Long memberId) {
        Member current = getCurrentMember();

        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID를 입력해주세요.");
        }

        Member target = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자", memberId));

        validateSameCompany(current, target);

        return MemberDetailResponse.from(target);
    }

    /** 회원 정보 수정 */
    @Transactional
    public MemberDetailResponse updateMember(Long memberId, MemberUpdateRequest request) {
        Member current = getCurrentMember();

        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID를 입력해주세요.");
        }

        Member target = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자", memberId));

        validateUpdatePermission(current, target);

        if (request.userName() != null && !request.userName().equals(target.getUserName()) &&
                memberRepository.existsByUserName(request.userName())) {
            throw new DuplicateException("아이디", request.userName());
        }

        if (request.email() != null && !request.email().equals(target.getEmail()) &&
                memberRepository.existsByEmail(request.email())) {
            throw new DuplicateException("이메일", request.email());
        }

        target.updateInfo(request.userName(), request.email(), request.phone());
        target.updateAddress(request.city(), request.street(), request.zipcode());

        if (request.password() != null && !request.password().trim().isEmpty()) {
            target.changePassword(passwordEncoder.encode(request.password()));
        }

        if (request.role() != null && !request.role().trim().isEmpty()) {
            if (!target.getRole().name().equals(request.role())) {
                validateRoleChangePermission(current, target, request.role());
                target.updateRole(Role.valueOf(request.role()));
            }
        }

        return MemberDetailResponse.from(target);
    }

    /** 회원 삭제(Soft Delete) */
    @Transactional
    public void deleteMember(Long memberId) {
        Member current = getCurrentMember();

        if (current.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("회원 삭제는 ADMIN만 가능합니다.");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("회원 ID를 입력해주세요.");
        }
        if (current.getId().equals(memberId)) {
            throw new IllegalArgumentException("자기 자신은 삭제할 수 없습니다.");
        }

        Member target = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자", memberId));

        validateSameCompany(current, target);

        target.softDelete();
    }
}
