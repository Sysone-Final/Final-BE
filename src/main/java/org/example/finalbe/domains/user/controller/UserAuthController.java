package org.example.finalbe.domains.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.user.dto.UserLoginRequest;
import org.example.finalbe.domains.user.dto.UserLoginResponse;
import org.example.finalbe.domains.user.dto.UserSignupRequest;
import org.example.finalbe.domains.user.dto.UserSignupResponse;
import org.example.finalbe.domains.user.service.UserAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    /**
     * 회원가입
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponse> signup(@RequestBody UserSignupRequest request) {
        UserSignupResponse response = userAuthService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로그인
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@RequestBody UserLoginRequest request) {
        UserLoginResponse response = userAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        userAuthService.logout(token);
        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }
}