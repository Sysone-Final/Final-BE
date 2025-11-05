package org.example.finalbe.domains.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
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
    // ObjectMapper는 CommonErrorDto를 위해 남겨둘 수 있지만, sendErrorResponse에선 사용하지 않습니다.
    // private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 1. permitAll 경로는 JWT 필터 스킵
        if (shouldSkipFilter(requestURI, method)) {
            log.debug("Skipping JWT filter for public path: {} {}", method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        // 2. 토큰이 없는 경우 (permitAll 경로는 위에서 걸러짐)
        if (token == null) {
            log.debug("No JWT token found for protected request: {}", requestURI);
            // 401 응답은 SecurityConfig의 .authenticationEntryPoint가 처리하도록 넘김
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // validateToken이 예외를 던진다고 가정 (가장 표준적인 방식)
            if (!jwtTokenProvider.validateToken(token)) {
                // validateToken이 boolean을 반환하고 예외를 던지지 않는 경우
                log.warn("Invalid JWT token (logical fail): {}", requestURI);
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
                return;
            }

            String userId = jwtTokenProvider.getUserId(token);
            String role = jwtTokenProvider.getRole(token);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("User authenticated: userId={}, role={}", userId, role);

        } catch (SignatureException | MalformedJwtException e) {
            log.warn("Invalid JWT signature/structure for request {}: {}", requestURI, e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "유효하지 않거나 손상된 토큰입니다.");
            return;
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token for request {}: {}", requestURI, e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.");
            return;
        } catch (Exception e) {
            log.error("JWT authentication failed for request: {}", requestURI, e);
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.");
            return;
        }

        // 4. 인증 성공, 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipFilter(String requestURI, String method) {
        // 인증 API는 항상 스킵
        if (requestURI.startsWith("/api/auth/")) {
            return true;
        }

        // GET /api/companies 스킵
        if ("GET".equalsIgnoreCase(method)) {
            if (requestURI.equals("/api/companies") || requestURI.equals("/api/companies/")) {
                return true;
            }
        }

        // /api/devices/** 모든 메서드 스킵
        if (requestURI.startsWith("/api/devices")) {
            return true;
        }

        return false;
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }


    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // ObjectMapper 대신 수동으로 JSON 문자열 생성 (CommonResDto 형식에 맞춤)
        String jsonResponse = String.format("{\"status_code\": %d, \"status_message\": \"%s\", \"result\": null}",
                status.value(), message);

        response.getWriter().write(jsonResponse);
    }
}