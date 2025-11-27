// 작성자: 황요한
// 로그아웃 완료 후 사용자 이름과 메시지를 담아 반환하는 DTO

package org.example.finalbe.domains.member.dto;

public record MemberLogoutResponse(
        String userName,
        String message
) {
}
