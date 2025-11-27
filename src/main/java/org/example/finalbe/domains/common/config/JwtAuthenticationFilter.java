/**
 * 작성자: 황요한
 * JWT 인증을 처리하는 필터 (로그인/회원가입 제외 모든 요청 검증)
 */
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String uri = request.getRequestURI();
            String method = request.getMethod();

            // 인증 필요 없는 경로는 바로 패스
            if (shouldSkipFilter(uri, method)) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = resolveToken(request);

            if (token == null) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
                return;
            }

            if (!jwtTokenProvider.validateToken(token)) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
                return;
            }

            authenticateUser(token, request);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT 인증 실패: {}", request.getRequestURI(), e);
            SecurityContextHolder.clearContext();

            if (shouldSkipFilter(request.getRequestURI(), request.getMethod())) {
                filterChain.doFilter(request, response);
            } else {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.");
            }
        }
    }

    /**
     * 인증 제외 경로
     */
    private boolean shouldSkipFilter(String uri, String method) {
        return uri.startsWith("/api/auth/");
    }

    /**
     * 요청 헤더에서 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (StringUtils.hasText(header) && header.startsWith("Bearer "))
                ? header.substring(7)
                : null;
    }

    /**
     * 토큰 기반 사용자 인증
     */
    private void authenticateUser(String token, HttpServletRequest request) {
        String userId = jwtTokenProvider.getUserId(token);
        String role = jwtTokenProvider.getRole(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 에러 응답 전송
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        CommonErrorDto errorDto = new CommonErrorDto(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorDto));
    }
}
