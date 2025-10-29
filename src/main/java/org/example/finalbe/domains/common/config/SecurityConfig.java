package org.example.finalbe.domains.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 클래스
 * JWT 기반 인증 및 권한 관리 설정
 *
 * 사용 기술:
 * - Spring Security 6.x: 웹 애플리케이션 보안 프레임워크
 * - JWT: 토큰 기반 인증 (세션 미사용)
 * - BCrypt: 비밀번호 암호화 알고리즘
 * - Stateless Architecture: 서버에 세션 상태 저장하지 않음
 */
@Configuration // Spring 설정 클래스임을 선언
// @Configuration은 해당 클래스가 하나 이상의 @Bean 메서드를 가지고 있음을 나타냄

@EnableWebSecurity // Spring Security 활성화
// Spring Security의 웹 보안 지원을 활성화하고, Spring MVC와 통합

@EnableMethodSecurity // 메서드 레벨 보안 활성화 (@PreAuthorize, @PostAuthorize 등 사용 가능)
// 컨트롤러나 서비스 메서드에 @PreAuthorize("hasRole('ADMIN')") 같은 어노테이션 사용 가능

@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성 (의존성 주입)
public class SecurityConfig {

    // === 의존성 주입 ===
    private final JwtAuthenticationFilter jwtAuthenticationFilter; // JWT 인증 필터
    // JwtAuthenticationFilter를 주입받아 UsernamePasswordAuthenticationFilter 앞에 추가

    /**
     * SecurityFilterChain 빈 설정
     * HTTP 보안 설정 및 인증/인가 규칙 정의
     *
     * @param http HttpSecurity 객체 (Spring Security가 자동 주입)
     * @return SecurityFilterChain (필터 체인 객체)
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean // 이 메서드가 반환하는 객체를 Spring Bean으로 등록
    // Spring Security 6부터는 SecurityFilterChain을 Bean으로 등록하는 방식 사용
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // === HTTP 보안 설정 ===
        http
                // === CSRF 비활성화 ===
                .csrf(csrf -> csrf.disable())
                // CSRF(Cross-Site Request Forgery) 보호 비활성화
                // 이유: JWT 토큰 기반 인증을 사용하므로 CSRF 토큰 불필요
                // JWT는 Authorization 헤더로 전달되어 브라우저가 자동으로 전송하지 않음
                // 만약 쿠키 기반 인증이라면 CSRF 활성화 필요

                // === 세션 관리 정책 ===
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // STATELESS: 서버에 세션 상태를 저장하지 않음 (무상태)
                // JWT를 사용하므로 서버는 세션을 생성하지 않고, 매 요청마다 토큰으로 인증
                // 장점: 확장성 good (로드 밸런싱 시 세션 공유 문제 없음)

                // === 요청별 인증/인가 규칙 ===
                .authorizeHttpRequests(auth -> auth
                                // authorizeHttpRequests: URL 패턴별로 접근 권한 설정

                                // === 인증 불필요 (Public Endpoints) ===
                                .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh").permitAll()
                                // permitAll(): 모든 사용자 접근 가능 (인증 불필요)
                                // 회원가입, 로그인, 토큰 재발급은 누구나 접근 가능해야 함

                                // 회사 목록 조회는 회원가입 시 필요하므로 인증 불필요
                                .requestMatchers(HttpMethod.GET, "/api/companies").permitAll()
                                // HttpMethod.GET: HTTP GET 메서드만 허용

                                // === 인증 필요 (Authenticated Endpoints) ===
                                .requestMatchers("/api/companies/**").authenticated()
                                // authenticated(): 인증된 사용자만 접근 가능 (로그인 필요)
                                // /**는 하위 모든 경로 포함

                                .requestMatchers("/api/datacenters/**").authenticated()
                                // 전산실 관련 API는 모두 인증 필요

                                .requestMatchers("/api/company-datacenters/**").authenticated()
                                // 회사-전산실 매핑 API도 인증 필요

                                .requestMatchers("/api/equipments/**").authenticated()
                                // 장비 관련 API 인증 필요

                                .requestMatchers("/api/devices/**").permitAll()
                                // 장치 API는 현재 인증 불필요 (개발 편의상, 운영에서는 authenticated()로 변경 권장)

                                .requestMatchers("/api/device-types/**").authenticated()
                                // 장치 타입 API 인증 필요

                                .requestMatchers("/api/departments/**").authenticated()
                                // 부서 관련 API 인증 필요

                                // === 그 외 모든 요청 ===
                                .anyRequest().authenticated()
                        // 위에서 정의하지 않은 모든 요청은 인증 필요
                        // 보안을 위해 기본적으로 모든 엔드포인트는 인증 필요하도록 설정
                )

                // === JWT 인증 필터 추가 ===
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // addFilterBefore: 지정한 필터 앞에 커스텀 필터 추가
        // JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 배치
        // 이유: JWT 토큰을 먼저 검증하고, 유효하면 SecurityContext에 인증 정보 저장
        // Spring Security Filter Chain 순서:
        // 1. SecurityContextPersistenceFilter
        // 2. JwtAuthenticationFilter (우리가 추가한 필터) ← 여기서 JWT 검증
        // 3. UsernamePasswordAuthenticationFilter (기본 Form Login 필터)
        // 4. 기타 필터들...
        // 5. FilterSecurityInterceptor (권한 검증)

        // === SecurityFilterChain 반환 ===
        return http.build();
        // build(): 설정된 HttpSecurity 객체를 기반으로 SecurityFilterChain 생성
    }

    /**
     * PasswordEncoder 빈 설정
     * 비밀번호 암호화 및 검증에 사용
     *
     * @return BCryptPasswordEncoder 객체
     *
     * BCrypt의 특징:
     * - Adaptive Hashing: 컴퓨터 성능이 발전해도 계산 비용을 증가시킬 수 있음
     * - Salt 자동 생성: 같은 비밀번호라도 매번 다른 해시 생성
     * - 단방향 암호화: 복호화 불가능, matches()로만 검증 가능
     */
    @Bean // Spring Bean으로 등록하여 Service에서 주입받아 사용
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
        // BCryptPasswordEncoder(strength): BCrypt 알고리즘으로 비밀번호 암호화
        // strength: 암호화 강도 (4~31, 기본값 10)
        // 12: 2^12번 해싱 (4096번), 숫자가 클수록 안전하지만 느림
        // 권장값: 10~12 (보안과 성능의 균형)

        // 사용 예시:
        // 암호화: passwordEncoder.encode("rawPassword") → "$2a$12$..."
        // 검증: passwordEncoder.matches("rawPassword", encodedPassword) → true/false
    }
}