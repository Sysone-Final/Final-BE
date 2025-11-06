package org.example.finalbe.domains.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.dto.MemberListResponse;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 회원 관리 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    /**
     * 로그인한 사용자가 속한 회사의 회원 목록 조회
     */
    public List<MemberListResponse> getMembersByCompany() {
        Member currentMember = getCurrentMember();
        Long companyId = currentMember.getCompany().getId();

        log.info("Fetching members for company: {} by user: {}",
                companyId, currentMember.getUserName());

        // 회사별 활성 회원 목록 조회
        List<Member> members = memberRepository.findActiveByCompanyId(companyId);

        log.info("Found {} members for company: {}", members.size(), companyId);

        return members.stream()
                .map(MemberListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원 상세 조회 (같은 회사 회원만 조회 가능)
     */
    public MemberListResponse getMemberById(Long memberId) {
        Member currentMember = getCurrentMember();

        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("사용자", memberId));

        // 같은 회사의 회원만 조회 가능
        if (!member.getCompany().getId().equals(currentMember.getCompany().getId())) {
            throw new AccessDeniedException("다른 회사의 회원 정보는 조회할 수 없습니다.");
        }

        return MemberListResponse.from(member);
    }

    // === Private Helper Methods ===

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
}