package org.example.finalbe.domains.member.dto;

import lombok.Builder;

/**
 * 로그아웃 응답 DTO
 * 로그아웃 성공 시 클라이언트에 반환할 데이터
 *
 * 로그아웃 처리 과정:
 * 1. 클라이언트가 로그아웃 요청 (Access Token을 헤더로, Refresh Token을 쿠키로 전달)
 * 2. 서버가 DB에서 Refresh Token 삭제 (완전한 무효화)
 * 3. 서버가 Refresh Token 쿠키 삭제 (MaxAge=0 설정)
 * 4. 클라이언트는 저장된 Access Token을 삭제 (로컬스토리지, 메모리 등)
 * 5. 로그아웃 성공 메시지 반환
 */
@Builder // Lombok의 빌더 패턴 적용
public record MemberLogoutResponse( // record: 불변 객체

                                    String message // 로그아웃 성공 메시지
                                    // "로그아웃이 완료되었습니다." 같은 안내 메시지

                                    // === 로그아웃은 단순하므로 메시지만 반환 ===
                                    // 추가 정보가 필요 없는 이유:
                                    // - 로그아웃 후에는 회원 정보를 표시할 필요가 없음
                                    // - 클라이언트는 로그인 페이지로 리다이렉트하거나 메인 페이지로 이동
) {
    /**
     * 간단한 메시지 응답 생성을 위한 정적 팩토리 메서드
     *
     * @param message 로그아웃 메시지
     * @return MemberLogoutResponse DTO
     *
     * of 메서드를 사용하는 이유:
     * - 간단한 객체 생성 시 of() 네이밍 컨벤션 사용
     * - from()은 다른 객체로부터 변환할 때 사용
     * - of()는 파라미터를 받아 새 객체를 생성할 때 사용
     */
    public static MemberLogoutResponse of(String message) {
        return MemberLogoutResponse.builder() // 빌더 패턴으로 DTO 생성
                .message(message) // 메시지 설정
                .build(); // 최종 DTO 객체 생성
    }
}