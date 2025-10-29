package org.example.finalbe.domains.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 로그인 요청 DTO
 * 클라이언트로부터 로그인 정보를 받는 데이터 전송 객체
 *
 * 사용 기술:
 * - Bean Validation: Jakarta Validation API로 입력값 검증
 * - Record: 불변 객체로 데이터 전달
 * - Lombok Builder: 빌더 패턴 적용
 */
@Builder // Lombok의 빌더 패턴 적용
// 빌더 패턴을 사용하면 MemberLoginRequest.builder().userName("test").password("pass").build() 형식으로 생성
public record MemberLoginRequest( // record: 불변 객체 (Java 14 이상)
                                  // record는 final 필드만 가지며, 자동으로 생성자, getter, equals, hashCode, toString 생성

                                  // === 아이디 필드 ===
                                  @NotBlank(message = "아이디를 입력해주세요.") // null, 빈 문자열, 공백 문자열 모두 불허
                                  // @NotBlank는 @NotNull + @NotEmpty + 공백 체크
                                  // 로그인 시 아이디는 필수 입력값

                                  @Size(max = 50, message = "아이디는 50자를 초과할 수 없습니다.") // 최대 길이 제한
                                  // 회원가입 시 userName의 최대 길이가 50이므로 동일하게 설정
                                  // 길이 제한으로 SQL Injection 등의 공격 방어
                                  String userName, // 로그인용 아이디

                                  // === 비밀번호 필드 ===
                                  @NotBlank(message = "비밀번호를 입력해주세요.") // 비밀번호는 필수
                                  // 로그인 시 비밀번호는 필수 입력값

                                  @Size(max = 100, message = "비밀번호는 100자를 초과할 수 없습니다.") // 최대 길이 제한
                                  // 회원가입 시 password의 최대 길이가 100이므로 동일하게 설정
                                  // 평문 비밀번호의 최대 길이 제한
                                  String password // 평문 비밀번호
                                  // 클라이언트에서 평문으로 전송 (HTTPS 사용 필수)
                                  // Service에서 passwordEncoder.matches()로 암호화된 비밀번호와 비교
) {
    // === 로그인 요청은 단순하므로 별도의 메서드 불필요 ===
    // toEntity() 같은 변환 메서드가 필요 없는 이유:
    // - 로그인은 기존 회원을 조회하는 것이지 새로운 엔티티를 생성하는 것이 아님
    // - Service에서 userName으로 회원을 조회하고 password를 검증만 함
}