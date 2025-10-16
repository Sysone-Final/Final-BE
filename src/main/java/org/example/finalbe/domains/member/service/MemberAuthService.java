package org.example.finalbe.domains.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
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
        log.info("Signup attempt for username: {}", request.username());

        // 중복 검증
        if (memberRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (request.email() != null && memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 회사 검증
        if (request.companyId() == null) {
            throw new IllegalArgumentException("회사를 선택해주세요.");
        }

        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회사입니다."));

        // DTO의 toEntity 메서드로 Member 엔티티 생성
        Member member = request.toEntity(
                passwordEncoder.encode(request.password()),
                company
        );

        memberRepository.save(member);

        log.info("Member created successfully: username={}, company={}",
                member.getUsername(), company.getName());

        // DTO의 from 메서드로 Response 생성
        return MemberSignupResponse.from(member, "회원가입이 완료되었습니다.");
    }

    public MemberLoginResponse login(MemberLoginRequest request) {
        log.info("Login attempt for username: {}", request.username());

        // 사용자 조회 (삭제되지 않은 사용자만)
        Member member = memberRepository.findActiveByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 계정 상태 확인
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        log.info("Login successful: username={}, company={}",
                member.getUsername(), member.getCompany().getName());

        // DTO의 from 메서드로 Response 생성
        return MemberLoginResponse.from(member, accessToken, refreshToken);
    }

    public MemberLogoutResponse logout(String token) {
        log.info("Logout attempt");

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String userId = jwtTokenProvider.getUserId(token);
        redisTemplate.opsForValue().set(
                "BLACKLIST:" + token,
                userId,
                Duration.ofHours(1)
        );

        log.info("Logout successful for user: {}", userId);
        return MemberLogoutResponse.of("로그아웃 성공");
    }
}