package org.example.finalbe.domains.member.dto;

import lombok.Builder;

/**
 * 토큰 재발급 응답 DTO
 * Access Token 재발급 성공 시 클라이언트에 반환할 데이터
 *
 * 토큰 재발급 흐름 (RTR: Refresh Token Rotation):
 * 1. 클라이언트의 Access Token이 만료됨
 * 2. 클라이언트가 /api/auth/refresh 요청 (Refresh Token은 쿠키로 자동 전송)
 * 3. 서버가 Refresh Token 검증 (DB에 저장된 토큰과 비교 + 만료 시간 확인)
 * 4. 검증 성공 시 새로운 Access Token과 새로운 Refresh Token 생성
 * 5. 새 Access Token은 응답 바디에, 새 Refresh Token은 쿠키로 전달
 * 6. 기존 Refresh Token은 DB에서 삭제되고 새 토큰으로 갱신 (1회용)
 *
 * RTR의 장점:
 * - Refresh Token이 탈취되어도 1회만 사용 가능
 * - 탈취된 토큰으로 재발급 시도 시 정상 사용자의 토큰도 무효화되어 이상 탐지 가능
 */
@Builder // Lombok의 빌더 패턴 적용
public record TokenRefreshResponse( // record: 불변 객체

                                    String accessToken, // 새로 발급된 Access Token
                                    // 클라이언트는 이 새로운 Access Token으로 기존 토큰을 교체해야 함
                                    // 새 Access Token의 유효 시간은 1시간 (재설정됨)

                                    String message // 성공 메시지

                                    // === Refresh Token은 포함하지 않음 ===
                                    // 새로운 Refresh Token은 HTTP-Only Cookie로 전달되므로 응답 바디에 포함하지 않음
                                    // Service에서 HttpServletResponse.addCookie()로 이미 쿠키에 추가됨
) {
    /**
     * Access Token을 받아 응답 DTO 생성
     *
     * @param accessToken 새로 발급된 Access Token
     * @return TokenRefreshResponse DTO
     */
    public static TokenRefreshResponse of(String accessToken) {
        return TokenRefreshResponse.builder() // 빌더 패턴으로 DTO 생성
                .accessToken(accessToken) // 새 Access Token 설정
                .message("토큰 재발급 성공") // 성공 메시지
                .build(); // 최종 DTO 객체 생성
    }
}