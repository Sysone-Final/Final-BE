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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 회원 인증 서비스
 * 회원가입, 로그인, 로그아웃, 토큰 재발급 기능 제공
 *
 * - JWT (JSON Web Token): Access Token과 Refresh Token 기반 인증
 * - BCrypt: 비밀번호 암호화 알고리즘
 * - HTTP-Only Cookie: XSS 공격 방어를 위해 Refresh Token을 쿠키로 전달
 * - Spring Transaction: @Transactional로 데이터 일관성 보장
 */
@Service // Spring의 Service Layer Bean으로 등록
@Slf4j // Lombok의 로깅 기능 (log.info(), log.error() 등 사용 가능)
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션 (성능 최적화)
// 읽기 전용 트랜잭션은 Dirty Checking을 하지 않아 성능이 향상됨
public class MemberAuthService {

    // === 의존성 주입 (생성자 주입) ===
    private final MemberRepository memberRepository; // 회원 데이터 접근
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화/검증 (BCrypt)
    private final JwtTokenProvider jwtTokenProvider; // JWT 토큰 생성/검증
    private final CompanyRepository companyRepository; // 회사 데이터 접근

    // === Cookie 설정 (환경별 동적 설정) ===
    @Value("${cookie.secure:false}") // application.yml에서 값 주입 (기본값: false)
    // 개발 환경(HTTP)에서는 false, 운영 환경(HTTPS)에서는 true로 설정
    private boolean cookieSecure; // Cookie의 Secure 속성 (HTTPS에서만 전송)

    // === 상수 정의 ===
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken"; // Cookie 이름
    private static final int REFRESH_TOKEN_COOKIE_AGE = 7 * 24 * 60 * 60; // 7일 (초 단위)
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7; // Refresh Token 유효 기간 (일 단위)

    /**
     * 회원가입
     */
    @Transactional // 쓰기 작업이므로 readOnly = false (기본값)
    // @Transactional은 메서드 실행 전에 트랜잭션을 시작하고, 정상 종료 시 commit, 예외 발생 시 rollback
    public MemberSignupResponse signup(MemberSignupRequest request) {
        // 로그 출력: 회원가입 시도 정보
        log.info("Signup attempt for userName: {}", request.userName());

        // === 1단계: 입력값 검증 ===
        // 아이디가 null이거나 빈 문자열이면 예외 발생
        if (request.userName() == null || request.userName().trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        // 비밀번호가 null이거나 빈 문자열이면 예외 발생
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        // 이름이 null이거나 빈 문자열이면 예외 발생
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }

        // === 2단계: 중복 검증 ===
        // 아이디가 이미 존재하는지 확인 (데이터베이스 조회)
        if (memberRepository.existsByUserName(request.userName())) {
            // 중복이면 DuplicateException 발생 (GlobalExceptionHandler에서 처리)
            throw new DuplicateException("아이디", request.userName());
        }
        // 이메일이 입력되었고, 이미 존재하는지 확인
        if (request.email() != null && !request.email().trim().isEmpty()
                && memberRepository.existsByEmail(request.email())) {
            throw new DuplicateException("이메일", request.email());
        }

        // === 3단계: 회사 검증 ===
        // 회사 ID가 null이면 예외 발생
        if (request.companyId() == null) {
            throw new IllegalArgumentException("회사를 선택해주세요.");
        }
        // 회사 ID로 활성 회사 조회 (존재하지 않으면 EntityNotFoundException 발생)
        Company company = companyRepository.findActiveById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("회사", request.companyId()));

        // === 4단계: Member 엔티티 생성 ===
        // DTO의 toEntity 메서드를 사용하여 엔티티로 변환
        // passwordEncoder.encode()로 비밀번호를 BCrypt로 암호화
        Member member = request.toEntity(
                passwordEncoder.encode(request.password()), // 평문 비밀번호를 BCrypt 해시로 변환
                company // 조회한 회사 엔티티 전달
        );

        // === 5단계: 데이터베이스 저장 ===
        // JpaRepository의 save() 메서드로 INSERT 쿼리 실행
        // save()는 영속성 컨텍스트에 엔티티를 저장하고, 트랜잭션 커밋 시 실제 DB에 반영
        memberRepository.save(member);

        // 로그 출력: 회원가입 성공
        log.info("Member created successfully: userName={}, company={}",
                member.getUserName(), company.getName());

        // === 6단계: 응답 DTO 생성 및 반환 ===
        // MemberSignupResponse DTO로 변환하여 Controller에 반환
        return MemberSignupResponse.from(member, "회원가입이 완료되었습니다.");
    }

    /**
     * 로그인
     * JWT Access Token과 Refresh Token을 생성하고, Refresh Token은 쿠키로 전달
     */
    @Transactional // 로그인 시 Refresh Token을 DB에 저장해야 하므로 쓰기 트랜잭션 필요
    public MemberLoginResponse login(MemberLoginRequest request, HttpServletResponse response) {
        // 로그 출력: 로그인 시도 정보
        log.info("Login attempt for userName: {}", request.userName());

        // === 1단계: 입력값 검증 ===
        // 아이디가 null이거나 빈 문자열이면 예외 발생
        if (request.userName() == null || request.userName().trim().isEmpty()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        // 비밀번호가 null이거나 빈 문자열이면 예외 발생
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        // === 2단계: 사용자 조회 ===
        // userName으로 활성 상태인 회원 조회
        // Optional의 orElseThrow()로 없으면 예외 발생
        Member member = memberRepository.findActiveByUserName(request.userName())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));
        // 보안상 "아이디가 존재하지 않습니다"와 "비밀번호가 틀렸습니다"를 구분하지 않음

        // === 3단계: 비밀번호 검증 ===
        // PasswordEncoder의 matches() 메서드로 평문 비밀번호와 암호화된 비밀번호 비교
        // BCrypt는 단방향 암호화이므로 복호화 불가, matches()로만 검증 가능
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // === 4단계: 계정 상태 확인 ===
        // 계정이 활성 상태가 아니면 로그인 불가
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        // === 5단계: JWT 토큰 생성 ===
        // Access Token 생성 (회원 ID와 권한 정보 포함, 1시간 유효)
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        // Refresh Token 생성 (회원 ID만 포함, 7일 유효)
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // === 6단계: Refresh Token을 DB에 저장 ===
        // Refresh Token과 만료 시간을 Member 엔티티에 저장
        LocalDateTime refreshTokenExpiryDate = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);
        // Member 엔티티의 비즈니스 메서드 호출하여 Refresh Token 업데이트
        member.updateRefreshToken(refreshToken, refreshTokenExpiryDate);
        // JPA의 Dirty Checking으로 트랜잭션 커밋 시 자동으로 UPDATE 쿼리 실행
        // 별도의 save() 호출 불필요

        // === 7단계: Refresh Token을 HTTP-Only Cookie로 설정 ===
        // createRefreshTokenCookie() 헬퍼 메서드로 쿠키 생성
        Cookie refreshTokenCookie = createRefreshTokenCookie(refreshToken);
        // HttpServletResponse에 쿠키 추가 (클라이언트에 전달됨)
        response.addCookie(refreshTokenCookie);

        // 로그 출력: 로그인 성공
        log.info("Login successful: userName={}, company={}",
                member.getUserName(), member.getCompany().getName());

        // === 8단계: 응답 DTO 생성 및 반환 ===
        // Access Token만 응답 바디에 포함 (Refresh Token은 쿠키로 전달)
        return MemberLoginResponse.from(member, accessToken);
    }

    /**
     * 로그아웃
     * Refresh Token을 DB에서 삭제하고 쿠키도 제거
     */
    @Transactional // Refresh Token 삭제를 위한 쓰기 트랜잭션
    public MemberLogoutResponse logout(String accessToken, String refreshToken, HttpServletResponse response) {
        // 로그 출력: 로그아웃 시도
        log.info("Logout attempt");

        // === 1단계: Access Token 형식 검증 ===
        // Access Token이 null이거나 빈 문자열이면 예외 발생
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new InvalidTokenException("Access Token이 제공되지 않았습니다.");
        }
        // "Bearer " 접두사 제거 (HTTP Header에서 "Bearer {token}" 형식으로 전달됨)
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7); // "Bearer " 이후의 실제 토큰 추출
        }

        // === 2단계: Access Token 유효성 검증 ===
        // JwtTokenProvider의 validateToken() 메서드로 토큰 유효성 확인
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new InvalidTokenException("유효하지 않은 Access Token입니다.");
        }

        // === 3단계: Access Token에서 사용자 ID 추출 ===
        // JWT의 Payload에서 subject(회원 ID) 추출
        String userId = jwtTokenProvider.getUserId(accessToken);

        // === 4단계: 회원 조회 ===
        // 사용자 ID로 활성 회원 조회
        Member member = memberRepository.findActiveById(Long.parseLong(userId))
                .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));

        // === 5단계: Refresh Token 삭제 ===
        // Member 엔티티의 clearRefreshToken() 메서드 호출
        // refreshToken과 refreshTokenExpiryDate를 null로 설정
        member.clearRefreshToken();
        // JPA Dirty Checking으로 UPDATE 쿼리 자동 실행

        // === 6단계: Refresh Token Cookie 삭제 ===
        // Cookie의 MaxAge를 0으로 설정하여 즉시 삭제
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null); // 쿠키 값을 null로 설정
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가 (XSS 방어)
        cookie.setSecure(cookieSecure); // HTTPS에서만 전송 (환경별 설정)
        cookie.setPath("/"); // 모든 경로에서 쿠키 전송
        cookie.setMaxAge(0); // 즉시 만료 (브라우저에서 쿠키 삭제)
        // HttpServletResponse에 쿠키 추가
        response.addCookie(cookie);

        // 로그 출력: 로그아웃 성공
        log.info("Logout successful for user: {} (Refresh Token deleted, Cookie cleared)", userId);

        // === 7단계: 응답 DTO 반환 ===
        return MemberLogoutResponse.of("로그아웃 성공");
    }

    /**
     * 토큰 재발급
     * Refresh Token을 검증하고 새로운 Access Token과 Refresh Token 발급
     */
    @Transactional // 새 Refresh Token을 DB에 저장해야 하므로 쓰기 트랜잭션
    public TokenRefreshResponse refreshAccessToken(String refreshToken, HttpServletResponse response) {
        // 로그 출력: 토큰 재발급 시도
        log.info("Token refresh attempt");

        // === 1단계: Refresh Token 존재 여부 확인 ===
        // Refresh Token이 null이거나 빈 문자열이면 예외 발생
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new InvalidTokenException("Refresh Token이 제공되지 않았습니다.");
        }

        // === 2단계: Refresh Token 유효성 검증 ===
        // JwtTokenProvider의 validateToken()으로 토큰 서명 및 만료 시간 확인
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        // === 3단계: Refresh Token에서 사용자 ID 추출 ===
        // JWT의 Payload에서 subject(회원 ID) 추출
        String userId = jwtTokenProvider.getUserId(refreshToken);

        // === 4단계: 회원 조회 및 Refresh Token 검증 ===
        // 회원 ID로 활성 회원 조회
        Member member = memberRepository.findActiveById(Long.parseLong(userId))
                .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));

        // === 5단계: DB에 저장된 Refresh Token과 비교 ===
        // Member 엔티티의 isRefreshTokenValid() 메서드로 검증
        // 저장된 토큰과 일치하고, 만료 시간이 현재 시간보다 이후인지 확인
        if (!member.isRefreshTokenValid(refreshToken)) {
            // 토큰이 일치하지 않거나 만료되었으면 예외 발생
            log.warn("Refresh token mismatch or expired for user: {}", userId);
            throw new InvalidTokenException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // === 6단계: 계정 상태 확인 ===
        // 계정이 활성 상태가 아니면 토큰 재발급 불가
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비활성화된 계정입니다.");
        }

        // === 7단계: 새로운 Access Token 생성 ===
        // 회원 ID와 권한으로 새로운 Access Token 생성 (1시간 유효)
        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());

        // === 8단계: 새로운 Refresh Token 생성 (RTR: Refresh Token Rotation) ===
        // RTR 전략: 보안 강화를 위해 Refresh Token도 새로 발급
        // 기존 Refresh Token은 1회 사용 후 무효화되어 탈취 위험 감소
        String newRefreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // === 9단계: 새 Refresh Token을 DB에 저장 ===
        // 새로운 만료 시간 계산 (현재 시간 + 7일)
        LocalDateTime newRefreshTokenExpiryDate = LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS);
        // Member 엔티티의 updateRefreshToken() 메서드로 갱신
        member.updateRefreshToken(newRefreshToken, newRefreshTokenExpiryDate);
        // JPA Dirty Checking으로 UPDATE 쿼리 자동 실행

        // === 10단계: 새 Refresh Token을 Cookie로 갱신 ===
        // 새로운 Refresh Token으로 쿠키 생성
        Cookie refreshTokenCookie = createRefreshTokenCookie(newRefreshToken);
        // HttpServletResponse에 쿠키 추가 (기존 쿠키 덮어쓰기)
        response.addCookie(refreshTokenCookie);

        // 로그 출력: 토큰 재발급 성공
        log.info("Token refresh successful for user: {} (new tokens issued, cookie updated)", userId);

        // === 11단계: 응답 DTO 반환 ===
        // 새로운 Access Token만 응답 바디에 포함 (Refresh Token은 쿠키로 전달)
        return TokenRefreshResponse.of(newAccessToken);
    }

    /**
     * Refresh Token Cookie 생성 헬퍼 메서드
     * HTTP-Only, Secure, SameSite 속성을 설정하여 보안 강화
     */
    private Cookie createRefreshTokenCookie(String refreshToken) {
        // === Cookie 생성 ===
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken); // 쿠키 이름과 값 설정

        // === Cookie 보안 속성 설정 ===
        cookie.setHttpOnly(true); // JavaScript에서 접근 불가 (XSS 공격 방어)
        // HttpOnly: document.cookie로 쿠키 값을 읽을 수 없음

        cookie.setSecure(cookieSecure); // HTTPS에서만 전송 (환경별 동적 설정)
        // 개발 환경(HTTP): false, 운영 환경(HTTPS): true
        // Secure가 true면 HTTP 요청 시 쿠키가 전송되지 않음

        cookie.setPath("/"); // 모든 경로에서 쿠키 전송
        // Path 설정으로 쿠키가 전송될 URL 경로 제한 가능

        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_AGE); // 유효 기간 설정 (7일)
        // MaxAge 단위는 초 (7 * 24 * 60 * 60 = 604800초)

        // cookie.setSameSite("Strict"); // CSRF 방어 (Spring 6 이상)
        // SameSite: 크로스 사이트 요청 시 쿠키 전송 제한 (CSRF 공격 방어)
        // Strict, Lax, None 옵션 있음

        // 로그 출력: 쿠키 생성 정보
        log.debug("Cookie created: secure={}", cookieSecure);

        return cookie; // 생성된 쿠키 반환
    }
}