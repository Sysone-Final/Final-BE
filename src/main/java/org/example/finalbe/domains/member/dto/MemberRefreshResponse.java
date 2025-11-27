// 작성자: 황요한
// 역할: 토큰 재발급 요청에 대한 응답을 담는 DTO
// - accessToken: 새로 발급된 Access Token
// - message: 재발급 결과 메시지

package org.example.finalbe.domains.member.dto;

public record MemberRefreshResponse(
        String accessToken,
        String message
) {
}
