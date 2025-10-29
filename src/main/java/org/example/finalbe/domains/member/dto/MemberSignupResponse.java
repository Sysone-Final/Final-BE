package org.example.finalbe.domains.member.dto;

import lombok.Builder;
import org.example.finalbe.domains.member.domain.Member;

/**
 * 회원가입 응답 DTO
 * 회원가입 성공 시 클라이언트에 반환할 데이터
 *
 * 응답 DTO의 역할:
 * - Entity의 모든 정보를 노출하지 않고 필요한 정보만 선택적으로 전달
 * - 보안: 비밀번호 같은 민감 정보는 응답에 포함하지 않음
 * - 계층 분리: Entity는 영속성 계층, DTO는 표현 계층
 */
@Builder // Lombok의 빌더 패턴 적용
public record MemberSignupResponse( // record: 불변 객체

                                    // === 회원 기본 정보 ===
                                    Long id, // 생성된 회원의 고유 ID (Primary Key)
                                    // 회원가입 후 생성된 ID를 클라이언트에 반환하면 이후 API 요청에서 사용 가능

                                    String userName, // 로그인용 아이디
                                    // 비밀번호는 보안상 응답에 포함하지 않음!

                                    String name, // 회원 이름

                                    String email, // 이메일 주소

                                    String role, // 권한 (ADMIN, OPERATOR, VIEWER)
                                    // Enum을 문자열로 변환하여 전달 (JSON 직렬화에 용이)

                                    String companyName, // 소속 회사명
                                    // Company 엔티티 전체를 반환하지 않고 회사명만 추출하여 전달
                                    // 이유: 클라이언트는 회사 ID보다 회사명이 더 유용

                                    String message // 성공 메시지
                                    // "회원가입이 완료되었습니다." 같은 안내 메시지
) {
    /**
     * Entity를 Response DTO로 변환하는 정적 팩토리 메서드
     *
     * @param member 회원 엔티티 객체
     * @param message 성공 메시지
     * @return MemberSignupResponse DTO 객체
     *
     * 정적 팩토리 메서드 패턴의 장점:
     * - 생성자보다 의미 있는 이름 사용 가능 (from, of, valueOf 등)
     * - 캐싱, 싱글톤 등 다양한 생성 전략 적용 가능
     * - 하위 타입 객체 반환 가능
     */
    public static MemberSignupResponse from(Member member, String message) {
        // === Entity에서 필요한 정보만 추출하여 DTO 생성 ===
        return MemberSignupResponse.builder() // 빌더 패턴으로 DTO 생성

                .id(member.getId()) // 회원 ID
                // getId(): Member 엔티티의 getter

                .userName(member.getUserName()) // 아이디

                .name(member.getName()) // 이름

                .email(member.getEmail()) // 이메일

                .role(member.getRole().name()) // 권한을 문자열로 변환
                // Enum.name(): Enum의 이름을 문자열로 반환
                // 예: Role.ADMIN -> "ADMIN"

                .companyName(member.getCompany().getName()) // 회사명
                // member.getCompany(): Member와 연관된 Company 엔티티 조회 (연관 관계 탐색)
                // .getName(): Company의 이름 추출
                // JPA의 지연 로딩이 설정되어 있으면 이 시점에 DB 조회 발생

                .message(message) // 성공 메시지

                .build(); // 최종 DTO 객체 생성
    }
}