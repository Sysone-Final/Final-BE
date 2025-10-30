package org.example.finalbe.domains.member.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.member.dto.*;
import org.example.finalbe.domains.member.service.MemberAuthService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 회원 인증 컨트롤러
 * 회원가입, 로그인, 로그아웃, 토큰 재발급 API 제공
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    /**
     * 회원가입
     * POST /api/auth/signup
     *
     * @param request 회원가입 요청 DTO
     * @return 생성된 회원 정보
     */
    @PostMapping("/signup")
    public ResponseEntity<CommonResDto> signup(@Valid @RequestBody MemberSignupRequest request) {
        MemberSignupResponse response = memberAuthService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "회원가입이 완료되었습니다.", response));
    }

    /**
     * 로그인
     * POST /api/auth/login
     * Refresh Token은 HTTP-Only Cookie로 전달
     *
     * @param request 로그인 요청 DTO
     * @param response HTTP 응답 (쿠키 설정용)
     * @return Access Token 및 회원 정보
     */
    @PostMapping("/login")
    public ResponseEntity<CommonResDto> login(
            @Valid @RequestBody MemberLoginRequest request,
            HttpServletResponse response) {
        MemberLoginResponse loginResponse = memberAuthService.login(request, response);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "로그인이 완료되었습니다.", loginResponse));
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     *
     * @param accessToken Access Token (헤더)
     * @param refreshToken Refresh Token (쿠키)
     * @param response HTTP 응답 (쿠키 삭제용)
     * @return 로그아웃 완료 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<CommonResDto> logout(
            @RequestHeader("Authorization") String accessToken,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        MemberLogoutResponse logoutResponse = memberAuthService.logout(accessToken, refreshToken, response);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "로그아웃이 완료되었습니다.", logoutResponse));
    }

    /**
     * 토큰 재발급
     * POST /api/auth/refresh
     *
     * @param refreshToken Refresh Token (쿠키)
     * @param response HTTP 응답 (새 쿠키 설정용)
     * @return 새로운 Access Token 및 Refresh Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<CommonResDto> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh Token이 필요합니다.");
        }
        MemberRefreshResponse refreshResponse = memberAuthService.refresh(refreshToken, response);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "토큰이 재발급되었습니다.", refreshResponse));
    }
}