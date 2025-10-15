package org.example.finalbe.domains.user.service;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.common.config.JwtTokenProvider;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.user.domain.User;
import org.example.finalbe.domains.user.dto.UserLoginRequest;
import org.example.finalbe.domains.user.dto.UserLoginResponse;
import org.example.finalbe.domains.user.dto.UserSignupRequest;
import org.example.finalbe.domains.user.dto.UserSignupResponse;
import org.example.finalbe.domains.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public UserSignupResponse signup(UserSignupRequest request) {
        // 중복 검증
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // DTO의 toEntity 메서드로 User 엔티티 생성
        User user = request.toEntity(
                UUID.randomUUID().toString(),
                passwordEncoder.encode(request.password())
        );

        userRepository.save(user);

        // DTO의 from 메서드로 Response 생성
        return UserSignupResponse.from(user, "회원가입이 완료되었습니다.");
    }

    public UserLoginResponse login(UserLoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 계정 상태 확인
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // DTO의 from 메서드로 Response 생성
        return UserLoginResponse.from(user, accessToken, refreshToken);
    }

    public void logout(String token) {
        // Bearer 제거
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 토큰 검증
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        // Redis에 블랙리스트 추가 (토큰 만료 시간까지)
        String userId = jwtTokenProvider.getUserId(token);
        redisTemplate.opsForValue().set(
                "BLACKLIST:" + token,
                userId,
                Duration.ofHours(1) // 토큰 만료 시간과 동일하게 설정
        );
    }
}