package org.example.finalbe.domains.member.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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

    // Redis Key Prefix 상수
    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "BLACKLIST:";

    // Cookie 설정 상수
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_COOKIE_AGE = 7 * 24 * 60 * 60; // 7일

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

        Member member = request.toEntity(
                passwordEncoder.encode(request.password()),
                company
        );

        memberRepository.save(member);

        log.info("Member created successfully: userName={}, company={}",
                member.getUserName(), company.getName());

        return MemberSignupResponse.from(member, "회원가입이 완료되었습니다.");
    }

    /**
     * 🆕 로그인 (httpOnly Cookie 적용)
     */
    @Transactional
    public MemberLoginResponse login(MemberLoginRequest request, HttpServletResponse response) {
        log.info("Login attempt for userName: {}", request.userName());

        // 입력값 검증
        if (request.userName() == null || request.userName().trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        // 사용자 조회
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

        // Redis에 Refresh Token 저장
        try {
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + member.getId(),
                    refreshToken,
                    Duration.ofDays(7)
            );
            log.info("Refresh token saved to Redis for user: {}", member.getId());
        } catch (Exception e) {
            log.error("Failed to save refresh token to Redis", e);
        }

        // 🆕 Refresh Token을 httpOnly Cookie로 설정
        Cookie refreshTokenCookie = createRefreshTokenCookie(refreshToken);
        response.addCookie(refreshTokenCookie);

        log.info("Login successful: userName={}, company={}",
                member.getUserName(), member.getCompany().getName());

        // Access Token만 반환 (Refresh Token은 Cookie로 전달)
        return MemberLoginResponse.from(member, accessToken);
    }

    /**
     * 🆕 로그아웃 (Cookie 삭제 포함)
     */
    @Transactional
    public MemberLogoutResponse logout(String accessToken, String refreshToken, HttpServletResponse response) {
        log.info("Logout attempt");

        // Access Token 형식 검증
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new InvalidTokenException("Access Token이 제공되지 않았습니다.");
        }

        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        // Access Token 유효성 검증
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new InvalidTokenException();
        }

        // 사용자 ID 추출
        String userId = jwtTokenProvider.getUserId(accessToken);

        try {
            // Access Token 블랙리스트 등록
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    userId,
                    Duration.ofHours(1)
            );

            // Refresh Token 삭제
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

            // 🆕 Refresh Token Cookie 삭제
            Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);  // 즉시 삭제
            response.addCookie(cookie);

            log.info("Logout successful for user: {} (Access Token blacklisted, Refresh Token deleted, Cookie cleared)", userId);
        } catch (Exception e) {
            log.error("Failed to process logout in Redis", e);
            throw new IllegalStateException("로그아웃 처리 중 오류가 발생했습니다.");
        }

        return MemberLogoutResponse.of("로그아웃 성공");
    }

    /**
     * 🆕 토큰 재발급 (Cookie 갱신 포함)
     */
    @Transactional
    public TokenRefreshResponse refreshAccessToken(String refreshToken, HttpServletResponse response) {
        log.info("Token refresh attempt");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new InvalidTokenException("Refresh Token이 제공되지 않았습니다.");
        }

        // Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // Refresh Token에서 사용자 ID 추출
        String userId = jwtTokenProvider.getUserId(refreshToken);

        // Redis에서 저장된 Refresh Token 조회
        String savedRefreshToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

        // Redis에 없거나 일치하지 않으면 에러
        if (savedRefreshToken == null) {
            throw new InvalidTokenException("만료되었거나 존재하지 않는 Refresh Token입니다.");
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            log.warn("Refresh token mismatch for user: {}", userId);
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // 사용자 정보 조회
        Member member = memberRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));

        // 계정 상태 확인
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());

        // 새 Refresh Token 생성 (RTR)
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        try {
            // Redis에 새 Refresh Token 갱신
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + member.getId(),
                    newRefreshToken,
                    Duration.ofDays(7)
            );

            // 🆕 새 Refresh Token을 Cookie로 갱신
            Cookie refreshTokenCookie = createRefreshTokenCookie(newRefreshToken);
            response.addCookie(refreshTokenCookie);

            log.info("Token refresh successful for user: {} (new tokens issued, cookie updated)", userId);
        } catch (Exception e) {
            log.error("Failed to update refresh token in Redis", e);
            throw new IllegalStateException("토큰 재발급 중 오류가 발생했습니다.");
        }

        // Access Token만 반환 (Refresh Token은 Cookie로 전달)
        return TokenRefreshResponse.of(newAccessToken);
    }

    /**
     * 🆕 Refresh Token Cookie 생성 헬퍼 메서드
     */
    private Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);   // JS 접근 불가 (XSS 방어)
        cookie.setSecure(true);     // HTTPS only (프로덕션)
        cookie.setPath("/");        // 모든 경로에서 전송
        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_AGE);  // 7일
        // cookie.setSameSite("Strict");  // CSRF 방어 (Spring 6+)

        return cookie;
    }
}