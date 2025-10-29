package org.example.finalbe.domains.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.dto.CommonErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에서 JWT 토큰을 검증하고 인증 정보를 SecurityContext에 저장
 *
 * - Spring Security Filter: OncePerRequestFilter를 상속하여 요청당 한 번만 실행
 * - JWT: Access Token을 검증하여 사용자 인증
 */
@Slf4j // Lombok의 로깅 기능
@Component // Spring Bean으로 등록
@RequiredArgsConstructor // final 필드에 대한 생성자 자동 생성
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // OncePerRequestFilter: 요청당 한 번만 실행되는 필터 (중복 실행 방지)

    // === 의존성 주입 ===
    private final JwtTokenProvider jwtTokenProvider; // JWT 토큰 검증 담당
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환용

    /**
     * 필터의 핵심 메서드
     * 모든 HTTP 요청에서 실행되어 JWT 토큰을 검증
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // filterChain.doFilter()를 호출하여 다음 필터로 요청 전달

        try {
            // === 1단계: HTTP 요청 헤더에서 JWT 토큰 추출 ===
            // resolveToken() 메서드로 "Authorization" 헤더에서 토큰 추출
            String token = resolveToken(request);

            // === 2단계: 토큰이 존재하면 검증 및 인증 처리 ===
            if (token != null) {
                // === 2-1: 토큰 유효성 검증 ===
                // JwtTokenProvider의 validateToken()으로 서명 및 만료 시간 확인
                if (!jwtTokenProvider.validateToken(token)) {
                    // 토큰이 유효하지 않으면 경고 로그 출력
                    log.warn("Invalid JWT token for request: {}", request.getRequestURI());
                    // 다음 필터로 요청 전달 (인증 실패이지만 계속 진행)
                    filterChain.doFilter(request, response);
                    return; // 메서드 종료
                }

                // === 2-2: 토큰에서 사용자 정보 추출 ===
                // JWT의 Payload에서 userId와 role 추출
                String userId = jwtTokenProvider.getUserId(token); // subject에서 회원 ID 추출
                String role = jwtTokenProvider.getRole(token); // claims에서 권한 추출

                // === 2-3: Spring Security 인증 객체 생성 ===
                // UsernamePasswordAuthenticationToken: Spring Security의 인증 객체
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, // principal: 인증된 사용자 ID
                        null, // credentials: 비밀번호 (이미 인증되었으므로 null)
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)) // authorities: 권한 목록
                        // Spring Security는 권한을 "ROLE_" 접두사로 시작하는 문자열로 관리
                );
                // WebAuthenticationDetails: 요청의 세부 정보 (IP, 세션 ID 등) 설정
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // === 2-4: SecurityContext에 인증 정보 저장 ===
                // SecurityContextHolder: 현재 스레드의 보안 컨텍스트 관리
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // 이후 컨트롤러나 서비스에서 @AuthenticationPrincipal로 userId 접근 가능

                // 로그 출력: 인증 성공
                log.debug("User authenticated: userId={}, role={}", userId, role);
            }

            // === 3단계: 다음 필터로 요청 전달 ===
            // filterChain.doFilter()로 다음 필터 또는 컨트롤러로 요청 전달
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // === 예외 처리: JWT 인증 실패 시 ===
            // 로그 출력: 인증 실패
            log.error("JWT authentication failed for request: {}", request.getRequestURI(), e);
            // SecurityContext 초기화 (인증 정보 제거)
            SecurityContextHolder.clearContext();
            // 클라이언트에 401 Unauthorized 응답 전송
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.");
        }
    }

    /**
     * HTTP 요청 헤더에서 JWT 토큰 추출
     * "Authorization: Bearer {token}" 형식에서 토큰만 추출
     */
    private String resolveToken(HttpServletRequest request) {
        // "Authorization" 헤더 값 가져오기
        String bearerToken = request.getHeader("Authorization");
        // StringUtils.hasText(): null이 아니고 빈 문자열이 아닌지 확인
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // "Bearer " 접두사 제거하고 실제 토큰만 반환
            return bearerToken.substring(7); // "Bearer " 이후의 문자열 (7번째 인덱스부터)
        }
        // 토큰이 없거나 형식이 올바르지 않으면 null 반환
        return null;
    }

    /**
     * 에러 응답 전송
     * JSON 형식으로 에러 정보를 클라이언트에 전달
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        // HTTP 상태 코드 설정 (401 Unauthorized)
        response.setStatus(status.value());
        // Content-Type을 JSON으로 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // 응답 인코딩을 UTF-8로 설정 (한글 깨짐 방지)
        response.setCharacterEncoding("UTF-8");

        // 에러 DTO 생성
        CommonErrorDto errorDto = new CommonErrorDto(status, message);
        // ObjectMapper로 DTO를 JSON 문자열로 변환하여 응답 바디에 작성
        response.getWriter().write(objectMapper.writeValueAsString(errorDto));
    }
}