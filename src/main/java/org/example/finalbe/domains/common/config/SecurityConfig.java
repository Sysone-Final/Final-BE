// src/main/java/org/example/finalbe/domains/common/config/SecurityConfig.java
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증 관련 - 인증 불필요
                        .requestMatchers("/auth/signup", "/auth/login").permitAll()

                        // 회원가입 시 회사 목록 조회 허용
                        .requestMatchers(HttpMethod.GET, "/companies").permitAll()

                        // 회사 API - 인증 필요 (세부 권한은 @PreAuthorize로 제어)
                        .requestMatchers("/companies/**").authenticated()

                        // 전산실 API - 인증 필요 (세부 권한은 @PreAuthorize로 제어)
                        .requestMatchers("/datacenters/**").authenticated()

                        // 회사-전산실 매핑 API - 인증 필요 (세부 권한은 @PreAuthorize로 제어)
                        .requestMatchers("/company-datacenters/**").authenticated()

                        .requestMatchers("/equipments/**").authenticated()

                        // 그 외 모든 요청 - 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}