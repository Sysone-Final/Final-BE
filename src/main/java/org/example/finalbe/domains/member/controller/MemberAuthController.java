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
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    /**
     * 회원가입 기능
     */
    @PostMapping("/signup")
    public ResponseEntity<CommonResDto> signup(@Valid @RequestBody MemberSignupRequest request) {
        MemberSignupResponse response = memberAuthService.signup(request);
        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.CREATED,
                "회원가입이 완료되었습니다.",
                response
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(commonResDto);
    }

    /**
     * 🆕 로그인 기능 (HttpServletResponse 추가)
     * Refresh Token은 httpOnly Cookie로 전달
     */
    @PostMapping("/login")
    public ResponseEntity<CommonResDto> login(
            @Valid @RequestBody MemberLoginRequest request,
            HttpServletResponse response) {

        MemberLoginResponse loginResponse = memberAuthService.login(request, response);

        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.OK,
                "로그인이 완료되었습니다.",
                loginResponse  // Access Token만 포함
        );
        return ResponseEntity.ok(commonResDto);
    }

    /**
     * 🆕 로그아웃 기능 (Cookie 삭제 포함)
     */
    @PostMapping("/logout")
    public ResponseEntity<CommonResDto> logout(
            @RequestHeader("Authorization") String accessToken,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        MemberLogoutResponse logoutResponse = memberAuthService.logout(accessToken, refreshToken, response);

        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.OK,
                "로그아웃이 완료되었습니다.",
                logoutResponse
        );
        return ResponseEntity.ok(commonResDto);
    }

    /**
     * 🆕 토큰 재발급 기능 (Cookie에서 자동 수신)
     * Refresh Token은 Cookie에서 자동으로 받아옴
     */
    @PostMapping("/refresh")
    public ResponseEntity<CommonResDto> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new CommonResDto(
                            HttpStatus.UNAUTHORIZED,
                            "Refresh Token이 없습니다. 다시 로그인해주세요.",
                            null
                    ));
        }

        TokenRefreshResponse refreshResponse = memberAuthService.refreshAccessToken(refreshToken, response);

        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.OK,
                "토큰 재발급이 완료되었습니다.",
                refreshResponse  // Access Token만 포함
        );
        return ResponseEntity.ok(commonResDto);
    }
}