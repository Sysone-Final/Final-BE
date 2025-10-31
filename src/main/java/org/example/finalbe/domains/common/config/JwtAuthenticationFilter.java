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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            String method = request.getMethod();

            // permitAll 경로는 JWT 필터 스킵
            if (shouldSkipFilter(requestURI, method)) {
                log.debug("Skipping JWT filter for public path: {} {}", method, requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            String token = resolveToken(request);

            if (token != null) {
                if (!jwtTokenProvider.validateToken(token)) {
                    log.warn("Invalid JWT token for request: {}", requestURI);
                    filterChain.doFilter(request, response);
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
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT authentication failed for request: {}", request.getRequestURI(), e);
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "인증에 실패했습니다.");
        }
    }

    private boolean shouldSkipFilter(String requestURI, String method) {
        // 인증 API는 항상 스킵
        if (requestURI.startsWith("/api/auth/")) {
            return true;
        }

        // GET /api/companies는 스킵 (끝에 슬래시가 있어도 허용)
        if ("GET".equalsIgnoreCase(method)) {
            // /api/companies 또는 /api/companies/ 모두 허용
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

        CommonErrorDto errorDto = new CommonErrorDto(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorDto));
    }
}