/**
 * 작성자: 황요한
 * Spring Security 설정 클래스
 */
package org.example.finalbe.domains.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @PostConstruct
    public void init() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String contentType = request.getHeader("Accept");

                            if (contentType != null && contentType.contains("text/event-stream")) {
                                if (!response.isCommitted()) {
                                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                    response.setContentType("text/event-stream;charset=UTF-8");
                                    response.getWriter().write("event: error\ndata: {\"error\":\"Unauthorized\"}\n\n");
                                    response.getWriter().flush();
                                }
                                return;
                            }

                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String contentType = request.getHeader("Accept");

                            if (contentType != null && contentType.contains("text/event-stream")) {
                                if (!response.isCommitted()) {
                                    response.setStatus(HttpStatus.FORBIDDEN.value());
                                    response.setContentType("text/event-stream;charset=UTF-8");
                                    response.getWriter().write("event: error\ndata: {\"error\":\"Forbidden\"}\n\n");
                                    response.getWriter().flush();
                                }
                                return;
                            }

                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"접근 권한이 없습니다.\"}");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies").permitAll()

                        .requestMatchers("/api/monitoring/subscribe/**").permitAll()
                        .requestMatchers("/api/prometheus/metrics/stream/**").permitAll()
                        .requestMatchers("/api/monitoring/server-room/stream/**").permitAll()
                        .requestMatchers("/api/alerts/subscribe").permitAll()
                        .requestMatchers("/api/alerts/*/subscribe").permitAll()


                        .requestMatchers("/api/companies/**").authenticated()
                        .requestMatchers("/api/serverroom/**").authenticated()
                        .requestMatchers("/api/company-serverroom/**").authenticated()
                        .requestMatchers("/api/equipments/**").authenticated()
                        .requestMatchers("/api/devices/**").authenticated()
                        .requestMatchers("/api/device-types/**").authenticated()
                        .requestMatchers("/api/departments/**").authenticated()
                        .requestMatchers("/api/monitoring/**").authenticated()
                        .requestMatchers("/api/members/**").authenticated()
                        .requestMatchers("/api/history/**").authenticated()
                        .requestMatchers("/api/prometheus/metrics/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://serverway.shop",
                "http://serverway.shop",
                "https://api.serverway.shop",
                "http://api.serverway.shop",
                "http://localhost:4173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}