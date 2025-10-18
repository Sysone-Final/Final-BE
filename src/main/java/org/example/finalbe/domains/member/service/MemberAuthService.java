package org.example.finalbe.domains.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.common.exception.InvalidTokenException;
import org.example.finalbe.domains.member.dto.*;
import org.example.finalbe.domains.common.config.JwtTokenProvider;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final CompanyRepository companyRepository;

    @Transactional
    public MemberSignupResponse signup(MemberSignupRequest request) {
        log.info("Signup attempt for userName: {}", request.userName());

        // 입력값 검증
        if (request.userName() == null || request.userName().trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }

        // 중복 검증
        if (memberRepository.existsByUserName(request.userName())) {
            throw new DuplicateException("아이디", request.userName());
        }
        if (request.email() != null && !request.email().trim().isEmpty()
                && memberRepository.existsByEmail(request.email())) {
            throw new DuplicateException("이메일", request.email());
        }

        // 회사 검증
        if (request.companyId() == null) {
            throw new IllegalArgumentException("회사를 선택해주세요.");
        }

        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));

        // DTO의 toEntity 메서드로 Member 엔티티 생성
        Member member = request.toEntity(
                passwordEncoder.encode(request.password()),
                company
        );

        memberRepository.save(member);

        log.info("Member created successfully: userName={}, company={}",
                member.getUserName(), company.getName());

        // DTO의 from 메서드로 Response 생성
        return MemberSignupResponse.from(member, "회원가입이 완료되었습니다.");
    }

    public MemberLoginResponse login(MemberLoginRequest request) {
        log.info("Login attempt for userName: {}", request.userName());

        // 입력값 검증
        if (request.userName() == null || request.userName().trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        // 사용자 조회 (삭제되지 않은 사용자만)
        Member member = memberRepository.findActiveByUserName(request.userName())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 계정 상태 확인
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        log.info("Login successful: userName={}, company={}",
                member.getUserName(), member.getCompany().getName());

        // DTO의 from 메서드로 Response 생성
        return MemberLoginResponse.from(member, accessToken, refreshToken);
    }

    public MemberLogoutResponse logout(String token) {
        log.info("Logout attempt");

        // 토큰 형식 검증
        if (token == null || token.trim().isEmpty()) {
            throw new InvalidTokenException("토큰이 제공되지 않았습니다.");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw new InvalidTokenException();
        }

        // 사용자 ID 추출
        String userId = jwtTokenProvider.getUserId(token);

        // Redis에 블랙리스트 등록
        try {
            redisTemplate.opsForValue().set(
                    "BLACKLIST:" + token,
                    userId,
                    Duration.ofHours(1)
            );
        } catch (Exception e) {
            log.error("Failed to add token to blacklist", e);
            throw new IllegalStateException("로그아웃 처리 중 오류가 발생했습니다.");
        }

        log.info("Logout successful for user: {}", userId);
        return MemberLogoutResponse.of("로그아웃 성공");
    }
}