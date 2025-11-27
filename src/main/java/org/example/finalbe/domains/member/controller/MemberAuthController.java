// 작성자: 황요한
// 회원 인증 관련 API를 제공하는 컨트롤러

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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    // 회원가입 요청을 처리
    @PostMapping("/signup")
    public ResponseEntity<CommonResDto> signup(@Valid @RequestBody MemberSignupRequest request) {
        MemberSignupResponse response = memberAuthService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResDto(HttpStatus.CREATED, "회원가입이 완료되었습니다.", response));
    }

    // 로그인 요청을 처리하여 토큰을 발급
    @PostMapping("/login")
    public ResponseEntity<CommonResDto> login(
            @Valid @RequestBody MemberLoginRequest request,
            HttpServletResponse response) {
        MemberLoginResponse loginResponse = memberAuthService.login(request, response);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "로그인이 완료되었습니다.", loginResponse));
    }

    // 로그아웃 요청을 처리하여 토큰을 무효화
    @PostMapping("/logout")
    public ResponseEntity<CommonResDto> logout(
            @RequestHeader("Authorization") String accessToken,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        MemberLogoutResponse logoutResponse = memberAuthService.logout(accessToken, refreshToken, response);
        return ResponseEntity.ok(
                new CommonResDto(HttpStatus.OK, "로그아웃이 완료되었습니다.", logoutResponse));
    }

    // Refresh Token을 사용해 새로운 토큰을 재발급
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
