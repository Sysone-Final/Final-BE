package org.example.finalbe.domains.Member.service;

import lombok.RequiredArgsConstructor;
import org.example.finalbe.domains.Member.dto.*;
import org.example.finalbe.domains.common.config.JwtTokenProvider;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.Member.domain.Member;
import org.example.finalbe.domains.Member.repository.MemberRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public MemberSignupResponse signup(MemberSignupRequest request) {
        // 중복 검증
        if (memberRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (request.email() != null && memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // DTO의 toEntity 메서드로 User 엔티티 생성
        Member member = request.toEntity(
                passwordEncoder.encode(request.password())
        );

        memberRepository.save(member);

        // DTO의 from 메서드로 Response 생성
        return MemberSignupResponse.from(member, "회원가입이 완료되었습니다.");
    }

    public MemberLoginResponse login(MemberLoginRequest request) {
        // 사용자 조회
        Member member = memberRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 계정 상태 확인
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        // DTO의 from 메서드로 Response 생성
        return MemberLoginResponse.from(member, accessToken, refreshToken);
    }

    public MemberLogoutResponse logout(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        String userId = jwtTokenProvider.getUserId(token);
        redisTemplate.opsForValue().set(
                "BLACKLIST:" + token,
                userId,
                Duration.ofHours(1)
        );

        return MemberLogoutResponse.of("로그아웃 성공");
    }
}