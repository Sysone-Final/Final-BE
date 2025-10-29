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
 * 회원 인증 관련 REST API 컨트롤러
 * 회원가입, 로그인, 로그아웃, 토큰 재발급 API 제공
 *
 * - Spring MVC: RESTful API 구현
 * - Bean Validation: @Valid로 요청 DTO 검증
 * - HTTP-Only Cookie: Refresh Token 전달
 */
@RestController // @Controller + @ResponseBody (모든 메서드가 JSON 응답 반환)
@RequestMapping("/api/auth") // 이 컨트롤러의 모든 API는 /api/auth로 시작
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Validated // 메서드 파라미터 검증 활성화
public class MemberAuthController {

    // === 의존성 주입 ===
    private final MemberAuthService memberAuthService; // 인증 비즈니스 로직 처리

    /**
     * 회원가입 API
     * POST /api/auth/signup
     */
    @PostMapping("/signup") // HTTP POST 메서드와 /signup 경로 매핑
    public ResponseEntity<CommonResDto> signup(@Valid @RequestBody MemberSignupRequest request) {
        // @Valid: Bean Validation으로 요청 DTO 검증 (제약조건 위반 시 400 Bad Request)
        // @RequestBody: HTTP 요청 바디의 JSON을 MemberSignupRequest 객체로 변환

        // === 1단계: Service 계층 호출 ===
        // 회원가입 로직 수행 (DB에 회원 저장)
        MemberSignupResponse response = memberAuthService.signup(request);

        // === 2단계: 공통 응답 DTO 생성 ===
        // CommonResDto: 상태 코드, 메시지, 데이터를 포함하는 표준 응답 형식
        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.CREATED, // 201 Created (리소스 생성 성공)
                "회원가입이 완료되었습니다.", // 성공 메시지
                response // 응답 데이터 (회원 정보)
        );

        // === 3단계: HTTP 응답 반환 ===
        // ResponseEntity: HTTP 상태 코드와 바디를 포함하는 응답 객체
        return ResponseEntity.status(HttpStatus.CREATED).body(commonResDto);
        // 201 Created 상태 코드와 함께 응답 반환
    }

    /**
     * 로그인 API
     * POST /api/auth/login
     * Refresh Token은 HTTP-Only Cookie로 전달, Access Token은 응답 바디에 포함
     */
    @PostMapping("/login") // HTTP POST 메서드와 /login 경로 매핑
    public ResponseEntity<CommonResDto> login(
            @Valid @RequestBody MemberLoginRequest request, // 로그인 요청 DTO (아이디, 비밀번호)
            HttpServletResponse response) { // Cookie 설정을 위한 HttpServletResponse 주입

        // === 1단계: Service 계층 호출 ===
        // 로그인 로직 수행 (비밀번호 검증, JWT 토큰 생성, Refresh Token DB 저장)
        // response 객체를 전달하여 Service에서 Cookie 설정 가능하게 함
        MemberLoginResponse loginResponse = memberAuthService.login(request, response);
        // Service에서 이미 response.addCookie()로 Refresh Token 쿠키 설정 완료

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.OK, // 200 OK (성공)
                "로그인이 완료되었습니다.", // 성공 메시지
                loginResponse  // 응답 데이터 (Access Token과 회원 정보 포함, Refresh Token은 제외)
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(commonResDto); // 200 OK와 함께 응답 반환
        // 클라이언트는 응답 바디에서 Access Token을 받고, 쿠키에서 Refresh Token을 자동으로 받음
    }

    /**
     * 로그아웃 API
     * POST /api/auth/logout
     * Access Token을 헤더로, Refresh Token을 쿠키로 받아 처리
     */
    @PostMapping("/logout") // HTTP POST 메서드와 /logout 경로 매핑
    public ResponseEntity<CommonResDto> logout(
            @RequestHeader("Authorization") String accessToken, // HTTP 헤더에서 Access Token 추출
            // @RequestHeader: HTTP 헤더의 특정 값을 메서드 파라미터로 바인딩

            @CookieValue(value = "refreshToken", required = false) String refreshToken, // 쿠키에서 Refresh Token 추출
            // @CookieValue: HTTP 쿠키의 특정 값을 메서드 파라미터로 바인딩
            // required = false: 쿠키가 없어도 예외 발생하지 않음 (null 허용)

            HttpServletResponse response) { // Cookie 삭제를 위한 HttpServletResponse 주입

        // === 1단계: Service 계층 호출 ===
        // 로그아웃 로직 수행 (Refresh Token DB 삭제, Cookie 삭제)
        MemberLogoutResponse logoutResponse = memberAuthService.logout(accessToken, refreshToken, response);
        // Service에서 이미 response.addCookie()로 Refresh Token 쿠키 삭제 완료

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.OK, // 200 OK
                "로그아웃이 완료되었습니다.", // 성공 메시지
                logoutResponse // 응답 데이터
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(commonResDto);
    }

    /**
     * 토큰 재발급 API
     * POST /api/auth/refresh
     * Refresh Token을 쿠키로 받아 새로운 Access Token과 Refresh Token 발급
     */
    @PostMapping("/refresh") // HTTP POST 메서드와 /refresh 경로 매핑
    public ResponseEntity<CommonResDto> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshToken, // 쿠키에서 Refresh Token 추출
            HttpServletResponse response) { // 새로운 Refresh Token 쿠키 설정을 위한 HttpServletResponse 주입

        // === 1단계: Service 계층 호출 ===
        // 토큰 재발급 로직 수행 (Refresh Token 검증, 새 토큰 생성, DB 업데이트)
        TokenRefreshResponse refreshResponse = memberAuthService.refreshAccessToken(refreshToken, response);
        // Service에서 이미 response.addCookie()로 새로운 Refresh Token 쿠키 설정 완료

        // === 2단계: 공통 응답 DTO 생성 ===
        CommonResDto commonResDto = new CommonResDto(
                HttpStatus.OK, // 200 OK
                "토큰 재발급이 완료되었습니다.", // 성공 메시지
                refreshResponse // 응답 데이터 (새로운 Access Token 포함)
        );

        // === 3단계: HTTP 응답 반환 ===
        return ResponseEntity.ok(commonResDto);
        // 클라이언트는 응답 바디에서 새 Access Token을 받고, 쿠키에서 새 Refresh Token을 자동으로 받음
    }
}