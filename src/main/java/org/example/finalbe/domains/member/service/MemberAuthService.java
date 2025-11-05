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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 회원 인증 서비스
 * JWT 기반 회원가입, 로그인, 로그아웃, 토큰 재발급 처리
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CompanyRepository companyRepository;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_COOKIE_AGE = 7 * 24 * 60 * 60;
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    /**
     * 회원가입
     */
    @Transactional
    public MemberSignupResponse signup(MemberSignupRequest request) {
        log.info("Signup attempt: {}", request.userName());

        validateSignupInput(request);
        checkDuplicates(request);

        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));

        Member member = request.toEntity(passwordEncoder.encode(request.password()), company);
        memberRepository.save(member);

        log.info("Member created: {}", member.getUserName());
        return MemberSignupResponse.from(member, "회원가입이 완료되었습니다.");
    }

    /**
     * 로그인
     */
    @Transactional
    public MemberLoginResponse login(MemberLoginRequest request, HttpServletResponse response) {
        log.info("Login attempt: {}", request.userName());

        validateLoginInput(request);

        Member member = memberRepository.findActiveByUserName(request.userName())
                .orElseThrow(() -> new EntityNotFoundException("사용자", request.userName()));

        String role = member.getRole().toString();

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), role);
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        LocalDateTime refreshTokenExpiryDate = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);
        member.updateRefreshToken(refreshToken, refreshTokenExpiryDate);

        setRefreshTokenCookie(response, refreshToken);

        log.info("Login success: {}", member.getUserName());
        return MemberLoginResponse.from(member, accessToken);
    }

    /**
     * 로그아웃
     */
    @Transactional
    public MemberLogoutResponse logout(String accessToken, String refreshToken, HttpServletResponse response) {
        log.info("Logout attempt");

        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new InvalidTokenException("유효하지 않은 Access Token입니다.");
        }

        String token = accessToken.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new InvalidTokenException("유효하지 않은 Access Token입니다.");
        }

        String userId = jwtTokenProvider.getUserId(token);
        Member member = memberRepository.findActiveById(Long.parseLong(userId))
                .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));

        member.clearRefreshToken();
        clearRefreshTokenCookie(response);

        log.info("Logout success: {}", member.getUserName());
        return new MemberLogoutResponse(member.getUserName(), "로그아웃되었습니다.");
    }

    /**
     * 토큰 재발급
     */
    @Transactional
    public MemberRefreshResponse refresh(String refreshToken, HttpServletResponse response) {
        log.info("Token refresh attempt");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        String userId = jwtTokenProvider.getUserId(refreshToken);
        Member member = memberRepository.findActiveById(Long.parseLong(userId))
                .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        String role = member.getRole().toString();

        if (!member.isRefreshTokenValid(refreshToken)) {
            log.warn("Refresh token mismatch: {}", userId);
            throw new InvalidTokenException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        LocalDateTime newExpiryDate = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);
        member.updateRefreshToken(newRefreshToken, newExpiryDate);

        setRefreshTokenCookie(response, newRefreshToken);

        log.info("Token refresh success: {}", member.getUserName());
        return new MemberRefreshResponse(newAccessToken, "토큰이 재발급되었습니다.");
    }

    // === Private Helper Methods ===

    private void validateSignupInput(MemberSignupRequest request) {
        if (request.userName() == null || request.userName().trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        if (request.companyId() == null) {
            throw new IllegalArgumentException("회사를 선택해주세요.");
        }
    }

    private void checkDuplicates(MemberSignupRequest request) {
        if (memberRepository.existsByUserName(request.userName())) {
            throw new DuplicateException("아이디", request.userName());
        }
        if (request.email() != null && !request.email().trim().isEmpty()
                && memberRepository.existsByEmail(request.email())) {
            throw new DuplicateException("이메일", request.email());
        }
    }

    private void validateLoginInput(MemberLoginRequest request) {
        if (request.userName() == null || request.userName().trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
    }


    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
//        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
//        cookie.setHttpOnly(true);
//        cookie.setSecure(cookieSecure);
//        cookie.setPath("/");
//        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_AGE);
//        response.addCookie(cookie);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_AGE)
                .sameSite("None")
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
//        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
//        cookie.setHttpOnly(true);
//        cookie.setSecure(cookieSecure);
//        cookie.setPath("/");
//        cookie.setMaxAge(0);
//        response.addCookie(cookie);
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
    }
}