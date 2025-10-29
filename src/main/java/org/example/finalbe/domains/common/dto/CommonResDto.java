package org.example.finalbe.domains.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;

/**
 * 공통 성공 응답 DTO
 * 모든 API의 성공 응답을 표준화
 *
 * 응답 구조:
 * {
 *   "status_code": 200,
 *   "status_message": "성공 메시지",
 *   "result": { ... }
 * }
 *
 * 사용 이유:
 * - API 응답 형식을 통일하여 클라이언트 개발 편의성 향상
 * - 상태 코드, 메시지, 데이터를 일관된 구조로 전달
 * - 프론트엔드에서 응답 처리 로직을 표준화 가능
 */
@Data // Lombok: getter, setter, toString, equals, hashCode 자동 생성
// @Data는 모든 필드에 대한 getter/setter를 생성하므로 가변 객체
// DTO는 데이터 전달 목적이므로 @Data 사용 적합

@NoArgsConstructor // 기본 생성자 자동 생성 (매개변수 없는 생성자)
// Jackson이 JSON을 역직렬화할 때 기본 생성자 필요
public class CommonResDto {

    // === 응답 필드 ===

    private int status_code; // HTTP 상태 코드 (200, 201, 400 등)
    // status_code: 응답의 성공/실패를 나타내는 숫자
    // 200: OK (성공)
    // 201: Created (생성 성공)
    // 400: Bad Request (잘못된 요청)
    // 401: Unauthorized (인증 실패)
    // 403: Forbidden (권한 없음)
    // 404: Not Found (리소스 없음)
    // 500: Internal Server Error (서버 오류)

    private String status_message; // 성공 메시지 또는 설명
    // status_message: 사용자에게 표시할 메시지
    // 예: "로그인이 완료되었습니다.", "회원가입이 완료되었습니다."

    private Object result = new ArrayList<>(); // 실제 응답 데이터 (기본값: 빈 리스트)
    // result: API의 실제 데이터를 담는 필드
    // Object 타입이므로 어떤 데이터든 담을 수 있음 (DTO, List, Map 등)
    // 기본값을 빈 ArrayList로 설정한 이유: null을 피하고 빈 배열 반환

    /**
     * 생성자: HTTP 상태, 메시지, 결과 데이터를 받아 초기화
     *
     * @param httpStatus HTTP 상태 (HttpStatus Enum)
     * @param message 성공 메시지
     * @param result 응답 데이터
     *
     * 사용 예시:
     * return new CommonResDto(HttpStatus.OK, "조회 성공", memberList);
     */
    public CommonResDto(HttpStatus httpStatus, String message, Object result) {
        // HttpStatus: Spring의 HTTP 상태 코드 Enum
        // HttpStatus.OK, HttpStatus.CREATED, HttpStatus.BAD_REQUEST 등

        this.status_code = httpStatus.value(); // HttpStatus Enum을 숫자로 변환
        // httpStatus.value(): Enum의 숫자 값 추출
        // 예: HttpStatus.OK.value() → 200

        this.status_message = message; // 메시지 설정

        this.result = result; // 응답 데이터 설정
    }

    // === 응답 예시 ===

    // 예시 1: 회원 목록 조회 성공
    // CommonResDto response = new CommonResDto(
    //     HttpStatus.OK,
    //     "회원 목록 조회 성공",
    //     List.of(member1, member2, member3)
    // );
    // →
    // {
    //   "status_code": 200,
    //   "status_message": "회원 목록 조회 성공",
    //   "result": [
    //     { "id": 1, "name": "홍길동", ... },
    //     { "id": 2, "name": "김철수", ... },
    //     { "id": 3, "name": "이영희", ... }
    //   ]
    // }

    // 예시 2: 로그인 성공
    // CommonResDto response = new CommonResDto(
    //     HttpStatus.OK,
    //     "로그인 성공",
    //     new MemberLoginResponse(...)
    // );
    // →
    // {
    //   "status_code": 200,
    //   "status_message": "로그인 성공",
    //   "result": {
    //     "id": 1,
    //     "userName": "user123",
    //     "accessToken": "eyJhbGc..."
    //   }
    // }

    // 예시 3: 데이터가 없는 성공 응답
    // CommonResDto response = new CommonResDto(
    //     HttpStatus.OK,
    //     "삭제 성공",
    //     null
    // );
    // →
    // {
    //   "status_code": 200,
    //   "status_message": "삭제 성공",
    //   "result": []  // 기본값이 빈 리스트
    // }
}