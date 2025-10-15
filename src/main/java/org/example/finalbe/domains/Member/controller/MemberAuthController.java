package org.example.finalbe.domains.Member.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.Member.dto.*;
import org.example.finalbe.domains.Member.service.MemberAuthService;
import org.example.finalbe.domains.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    /**
     * 회원가입
     * POST /auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<CommonResDto> signup(@RequestBody MemberSignupRequest request) {
        MemberSignupResponse response = memberAuthService.signup(request);
        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.CREATED,
                "회원가입이 완료되었습니다.",
                response
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(commonResDto);
    }

    /**
     * 로그인
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<CommonResDto> login(@RequestBody MemberLoginRequest request) {
        MemberLoginResponse response = memberAuthService.login(request);
        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.OK,
                "로그인이 완료되었습니다.",
                response
        );
        return ResponseEntity.ok(commonResDto);
    }

    /**
     * 로그아웃
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<CommonResDto> logout(@RequestHeader("Authorization") String token) {
        MemberLogoutResponse response = memberAuthService.logout(token);
        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.OK,
                "로그아웃이 완료되었습니다.",
                response
        );
        return ResponseEntity.ok(commonResDto);
    }
}