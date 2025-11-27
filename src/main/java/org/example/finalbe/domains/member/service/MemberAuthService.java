// 작성자: 황요한
// 회원 인증 서비스: 회원가입, 로그인, 로그아웃, 토큰 재발급 처리

package org.example.finalbe.domains.member.service;

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

    /** 회원가입 처리 */
    @Transactional
    public MemberSignupResponse signup(MemberSignupRequest request) {
        validateSignupInput(request);
        checkDuplicates(request);

        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));

        Member member = request.toEntity(passwordEncoder.encode(request.password()), company);
        memberRepository.save(member);

        return MemberSignupResponse.from(member, "회원가입이 완료되었습니다.");
    }

    /** 로그인 처리 + 리프레시 토큰 발급 */
    @Transactional
    public MemberLoginResponse login(MemberLoginRequest request, HttpServletResponse response) {
        validateLoginInput(request);

        Member member = memberRepository.findActiveByUserName(request.userName())
                .orElseThrow(() -> new EntityNotFoundException("사용자", request.userName()));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        LocalDateTime expiry = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);

        member.updateRefreshToken(refreshToken, expiry);
        setRefreshTokenCookie(response, refreshToken);

        return MemberLoginResponse.from(member, accessToken);
    }

    /** 로그아웃 처리 */
    @Transactional
    public MemberLogoutResponse logout(String accessToken, String refreshToken, HttpServletResponse response) {
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

        return new MemberLogoutResponse(member.getUserName(), "로그아웃되었습니다.");
    }

    /** 토큰 재발급 처리 */
    @Transactional
    public MemberRefreshResponse refresh(String refreshToken, HttpServletResponse response) {

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        String userId = jwtTokenProvider.getUserId(refreshToken);
        Member member = memberRepository.findActiveById(Long.parseLong(userId))
                .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));

        if (!member.isRefreshTokenValid(refreshToken)) {
            throw new InvalidTokenException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        LocalDateTime expiry = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);

        member.updateRefreshTokenOnly(newRefreshToken, expiry);
        setRefreshTokenCookie(response, newRefreshToken);

        return new MemberRefreshResponse(newAccessToken, "토큰이 재발급되었습니다.");
    }

    /** 회원가입 입력값 검증 */
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

    /** 중복 데이터 검사 */
    private void checkDuplicates(MemberSignupRequest request) {
        if (memberRepository.existsByUserName(request.userName())) {
            throw new DuplicateException("아이디", request.userName());
        }
        if (request.email() != null && !request.email().isEmpty()
                && memberRepository.existsByEmail(request.email())) {
            throw new DuplicateException("이메일", request.email());
        }
    }

    /** 로그인 입력값 검증 */
    private void validateLoginInput(MemberLoginRequest request) {
        if (request.userName() == null || request.userName().trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
    }

    /** 리프레시 토큰 쿠키 생성 */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_AGE)
                .sameSite("None")
                .build();
        response.setHeader("Set-Cookie", cookie.toString());
    }

    /** 리프레시 토큰 쿠키 삭제 */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
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
