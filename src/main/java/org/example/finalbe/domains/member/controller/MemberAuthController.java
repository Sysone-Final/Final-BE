package org.example.finalbe.domains.member.controller;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.member.dto.*;
import org.example.finalbe.domains.member.service.MemberAuthService;
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
     * 회원가입 기능
     * 새로운 사용자를 시스템에 등록
     * 이메일, 비밀번호, 이름, 소속 회사 등의 정보를 받아서 계정 생성
     * 권한: 인증 불필요 (누구나 가입 가능)
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
     * 로그인 기능
     * 이메일과 비밀번호로 사용자 인증
     * 성공 시 JWT 액세스 토큰과 리프레시 토큰을 발급
     * 이후 API 호출 시 이 토큰을 사용하여 인증
     * 권한: 인증 불필요 (누구나 로그인 시도 가능)
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
     * 로그아웃 기능
     * 현재 로그인된 사용자의 세션을 종료
     * 발급된 토큰을 무효화하여 더 이상 API 접근 불가하게 만듦
     * Authorization 헤더에 토큰을 포함하여 요청
     * 권한: 로그인된 사용자만 가능
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