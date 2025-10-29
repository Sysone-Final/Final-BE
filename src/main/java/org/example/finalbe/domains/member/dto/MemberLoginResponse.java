package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.member.domain.Member;

/**
 * 로그인 응답 DTO
 * 로그인 성공 시 클라이언트에 반환할 데이터
 *
 * 중요: Refresh Token은 HTTP-Only Cookie로 전달되므로 이 DTO에는 포함하지 않음
 *
 * JWT 인증 흐름:
 * 1. 클라이언트가 로그인 요청 (userName, password)
 * 2. 서버가 인증 성공 시 Access Token과 Refresh Token 생성
 * 3. Access Token은 응답 바디(JSON)에 포함하여 전달
 * 4. Refresh Token은 HTTP-Only Cookie에 담아 전달 (JavaScript 접근 불가)
 * 5. 클라이언트는 Access Token을 저장하고 API 요청 시 Authorization 헤더에 포함
 * 6. 브라우저는 Refresh Token 쿠키를 자동으로 관리하고 요청 시 자동 전송
 */
@Builder // Lombok의 빌더 패턴 적용
public record MemberLoginResponse( // record: 불변 객체

                                   // === 회원 정보 ===
                                   Long id, // 회원 ID
                                   // 로그인 후 회원 ID를 알면 프로필 조회 등의 API 요청에 사용 가능

                                   String userName, // 아이디

                                   String name, // 이름
                                   // 화면에 "OOO님 환영합니다" 같은 메시지 표시 시 사용

                                   String email, // 이메일

                                   String role, // 권한
                                   // 클라이언트에서 권한에 따라 화면 구성을 다르게 할 때 사용
                                   // 예: ADMIN은 관리 메뉴 표시, VIEWER는 조회만 가능

                                   String companyName, // 회사명

                                   // === JWT 토큰 ===
                                   String accessToken, // Access Token (클라이언트가 매 요청마다 사용)
                                   // Access Token: 짧은 유효 시간(1시간), 사용자 인증 및 권한 확인에 사용
                                   // 클라이언트는 이 토큰을 저장하고 API 요청 시 Authorization 헤더에 "Bearer {accessToken}" 형식으로 전달
                                   // 예: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

                                   String message // 성공 메시지

                                   // === Refresh Token은 포함하지 않음 ===
                                   // Refresh Token은 HTTP-Only Cookie로 전달되므로 응답 바디에 포함하지 않음
                                   // 이유:
                                   // 1. XSS 공격 방어: JavaScript로 Refresh Token 접근 불가
                                   // 2. CSRF 공격은 SameSite Cookie 속성으로 방어
                                   // 3. Refresh Token은 Access Token 재발급에만 사용되므로 클라이언트가 직접 다룰 필요 없음
) {
    /**
     * Entity와 Access Token을 받아 Response DTO로 변환
     *
     * @param member 로그인한 회원 엔티티
     * @param accessToken 생성된 Access Token
     * @return MemberLoginResponse DTO
     */
    public static MemberLoginResponse from(Member member, String accessToken) {
        // === Entity와 Token에서 필요한 정보 추출하여 DTO 생성 ===
        return MemberLoginResponse.builder() // 빌더 패턴으로 DTO 생성

                .id(member.getId()) // 회원 ID

                .userName(member.getUserName()) // 아이디

                .name(member.getName()) // 이름

                .email(member.getEmail()) // 이메일

                .role(member.getRole().name()) // 권한을 문자열로 변환

                .companyName(member.getCompany().getName()) // 회사명

                .accessToken(accessToken) // Access Token만 응답에 포함
                // Refresh Token은 Service에서 HttpServletResponse.addCookie()로 이미 쿠키에 추가됨

                .message("로그인 성공") // 성공 메시지

                .build(); // 최종 DTO 객체 생성
    }
}