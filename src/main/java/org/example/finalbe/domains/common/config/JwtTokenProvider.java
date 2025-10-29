package org.example.finalbe.domains.common.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 담당 클래스
 *
 * - JJWT (Java JWT): JWT 토큰 생성 및 파싱 라이브러리
 * - HMAC-SHA256: JWT 서명 알고리즘
 * - Spring @Value: application.yml의 설정 값 주입
 */
@Slf4j // Lombok의 로깅 기능
@Component // Spring Bean으로 등록
public class JwtTokenProvider {

    // === JWT 설정 값 (불변) ===
    private final SecretKey secretKey; // JWT 서명에 사용할 비밀 키 (HMAC-SHA256)
    private final long accessTokenValidityInMilliseconds; // Access Token 유효 시간 (밀리초)
    private final long refreshTokenValidityInMilliseconds; // Refresh Token 유효 시간 (밀리초)

    /**
     * 생성자: application.yml의 설정 값을 주입받아 초기화
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret, // application.yml의 jwt.secret 값 주입
            @Value("${jwt.access-token-validity}") long accessTokenValidity, // Access Token 유효 시간 (밀리초)
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity) { // Refresh Token 유효 시간 (밀리초)

        // === Secret Key 길이 검증 ===
        // HMAC-SHA256은 최소 256비트(32바이트) 이상의 키가 필요
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
        }

        // === Secret Key 생성 ===
        // 문자열을 바이트 배열로 변환 후 HMAC-SHA256 키 생성
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // Keys.hmacShaKeyFor(): JJWT에서 제공하는 안전한 키 생성 메서드

        // === 유효 시간 설정 ===
        this.accessTokenValidityInMilliseconds = accessTokenValidity;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidity;

        // 로그 출력: 초기화 완료
        log.info("JwtTokenProvider initialized with access token validity: {}ms", accessTokenValidity);
    }

    /**
     * Access Token 생성
     * 사용자 인증 및 권한 확인에 사용 (짧은 유효 시간)
     */
    public String createAccessToken(Long userId, String role) {
        // === 현재 시간과 만료 시간 계산 ===
        Date now = new Date(); // 현재 시간
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds); // 만료 시간 (현재 시간 + 유효 기간)

        // === JWT 토큰 생성 ===
        return Jwts.builder() // JWT 빌더 시작
                .subject(String.valueOf(userId)) // subject: 토큰의 주체 (회원 ID)
                // subject는 JWT의 표준 claim으로, 토큰이 누구에 관한 것인지 나타냄

                .claim("role", role) // 사용자 정의 claim: 권한 정보 추가
                // claim: JWT의 Payload에 포함되는 데이터 (key-value 형식)

                .issuedAt(now) // iat (Issued At): 토큰 발급 시간
                // JWT 표준 claim: 토큰이 언제 생성되었는지 기록

                .expiration(validity) // exp (Expiration): 토큰 만료 시간
                // JWT 표준 claim: 이 시간 이후로는 토큰이 유효하지 않음

                .signWith(secretKey) // 서명: Secret Key로 HMAC-SHA256 서명
                // signWith()는 JWT의 무결성을 보장하기 위해 토큰에 서명을 추가
                // 서명이 있으면 토큰이 위변조되었는지 검증 가능

                .compact(); // JWT 문자열로 변환 (Header.Payload.Signature 형식)
        // compact(): 최종적으로 JWT 토큰 문자열 생성
        // 예: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNjMwMDAwMDAwLCJleHAiOjE2MzAwMDM2MDB9.signature
    }

    /**
     * Refresh Token 생성
     * Access Token 재발급에 사용 (긴 유효 시간)
     */
    public String createRefreshToken(Long userId) {
        // === 현재 시간과 만료 시간 계산 ===
        Date now = new Date(); // 현재 시간
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds); // 만료 시간

        // === JWT 토큰 생성 ===
        return Jwts.builder() // JWT 빌더 시작
                .subject(String.valueOf(userId)) // subject: 회원 ID만 포함
                // Refresh Token은 Access Token과 달리 권한(role) 정보를 포함하지 않음
                // 이유: Refresh Token은 단순히 새로운 Access Token을 발급받기 위한 용도이므로 최소한의 정보만 포함

                .issuedAt(now) // 발급 시간
                .expiration(validity) // 만료 시간 (7일)
                .signWith(secretKey) // 서명
                .compact(); // JWT 문자열로 변환
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * Access Token과 Refresh Token 모두 사용 가능
     */
    public String getUserId(String token) {
        // === JWT 파싱 및 검증 ===
        return Jwts.parser() // JWT 파서 시작
                .verifyWith(secretKey) // Secret Key로 서명 검증
                // verifyWith(): 토큰의 서명이 유효한지 확인 (위변조 검증)
                // 서명이 일치하지 않으면 SignatureException 발생

                .build() // 파서 빌드

                .parseSignedClaims(token) // JWT 토큰 파싱 (서명 검증 포함)
                // parseSignedClaims(): 토큰을 파싱하고 서명을 검증
                // 만료된 토큰이면 ExpiredJwtException 발생

                .getPayload() // JWT의 Payload(Claims) 가져오기
                // Payload: JWT의 실제 데이터가 담긴 부분

                .getSubject(); // subject(회원 ID) 추출
        // getSubject(): JWT의 subject claim 값 반환
    }

    /**
     * JWT 토큰에서 권한(Role) 추출
     * Access Token에서 사용자의 권한 정보를 가져옴
     */
    public String getRole(String token) {
        // === JWT 파싱 및 검증 ===
        return Jwts.parser() // JWT 파서 시작
                .verifyWith(secretKey) // Secret Key로 서명 검증
                .build() // 파서 빌드
                .parseSignedClaims(token) // JWT 토큰 파싱
                .getPayload() // Payload 가져오기
                .get("role", String.class); // "role" claim 값 추출 (String 타입으로)
        // get(): 사용자 정의 claim 값을 가져옴
        // 두 번째 인자는 타입 지정 (String.class)
    }

    /**
     * JWT 토큰 유효성 검증
     * 서명, 만료 시간, 형식 등을 확인
     */
    public boolean validateToken(String token) {
        try {
            // === JWT 파싱 시도 ===
            Jwts.parser() // JWT 파서 시작
                    .verifyWith(secretKey) // Secret Key로 서명 검증
                    .build() // 파서 빌드
                    .parseSignedClaims(token); // JWT 토큰 파싱
            // 파싱이 성공하면 토큰이 유효함

            return true; // 유효한 토큰

        } catch (ExpiredJwtException e) {
            // === 토큰 만료 예외 ===
            // JWT의 exp(만료 시간)이 현재 시간보다 이전인 경우
            log.warn("Expired JWT token");

        } catch (UnsupportedJwtException e) {
            // === 지원하지 않는 JWT 형식 ===
            // JWT의 형식이 올바르지 않은 경우
            log.warn("Unsupported JWT token");

        } catch (MalformedJwtException e) {
            // === 잘못된 JWT 구조 ===
            // JWT가 Header.Payload.Signature 형식이 아닌 경우
            log.warn("Invalid JWT token");

        } catch (SecurityException e) {
            // === 서명 검증 실패 ===
            // Secret Key가 일치하지 않아 서명 검증에 실패한 경우 (위변조된 토큰)
            log.warn("Invalid JWT signature");

        } catch (IllegalArgumentException e) {
            // === JWT claims가 비어있음 ===
            // JWT 문자열이 null이거나 빈 문자열인 경우
            log.warn("JWT claims string is empty");
        }

        return false; // 유효하지 않은 토큰
    }
}