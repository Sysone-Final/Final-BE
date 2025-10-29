package org.example.finalbe.domains.datacenter.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DataCenterStatus;

import java.math.BigDecimal;

/**
 * 전산실 수정 요청 DTO
 *
 * - Record: 불변 객체로 DTO를 정의 (Java 14+)
 * - 부분 수정 지원: 모든 필드가 선택적(Optional)이며, null이 아닌 값만 업데이트
 * - Bean Validation: 입력값이 있을 때만 검증 수행
 * - Builder 패턴: 가독성 높은 객체 생성 지원
 */
@Builder // 빌더 패턴을 사용하여 가독성 높은 객체 생성
public record DataCenterUpdateRequest(
        @Size(max = 100, message = "전산실 이름은 100자 이내로 입력해주세요.") // 최대 100자 제한
        String name, // 전산실 이름 (선택, null이면 기존 값 유지)

        @Size(max = 50, message = "전산실 코드는 50자 이내로 입력해주세요.") // 최대 50자 제한
        String code, // 전산실 코드 (선택, null이면 기존 값 유지)

        @Size(max = 255, message = "위치는 255자 이내로 입력해주세요.") // 최대 255자 제한
        String location, // 전산실 위치/주소 (선택, null이면 기존 값 유지)

        @Size(max = 50, message = "층수는 50자 이내로 입력해주세요.") // 최대 50자 제한
        String floor, // 전산실 층수 (선택, null이면 기존 값 유지)

        @Min(value = 1, message = "행 수는 1 이상이어야 합니다.") // 최소값 1
        Integer rows, // 랙 배치 행 수 (선택, null이면 기존 값 유지)

        @Min(value = 1, message = "열 수는 1 이상이어야 합니다.") // 최소값 1
        Integer columns, // 랙 배치 열 수 (선택, null이면 기존 값 유지)

        @Size(max = 500, message = "배경 이미지 URL은 500자 이내로 입력해주세요.") // 최대 500자 제한
        String backgroundImageUrl, // 배경 이미지 URL (선택, null이면 기존 값 유지)

        DataCenterStatus status, // 전산실 상태 (선택, null이면 기존 값 유지)

        String description, // 전산실 설명 (선택, null이면 기존 값 유지)

        @DecimalMin(value = "0.0", message = "면적은 0 이상이어야 합니다.") // 최소값 0
        @Digits(integer = 10, fraction = 2, message = "면적은 정수 10자리, 소수점 2자리까지 입력 가능합니다.") // 정수 10자리, 소수점 2자리
        BigDecimal totalArea, // 총 면적 (선택, null이면 기존 값 유지)

        @DecimalMin(value = "0.0", message = "전력 용량은 0 이상이어야 합니다.") // 최소값 0
        @Digits(integer = 10, fraction = 2, message = "전력 용량은 정수 10자리, 소수점 2자리까지 입력 가능합니다.") // 정수 10자리, 소수점 2자리
        BigDecimal totalPowerCapacity, // 총 전력 용량 (선택, null이면 기존 값 유지)

        @DecimalMin(value = "0.0", message = "냉각 용량은 0 이상이어야 합니다.") // 최소값 0
        @Digits(integer = 10, fraction = 2, message = "냉각 용량은 정수 10자리, 소수점 2자리까지 입력 가능합니다.") // 정수 10자리, 소수점 2자리
        BigDecimal totalCoolingCapacity, // 총 냉각 용량 (선택, null이면 기존 값 유지)

        @Min(value = 1, message = "최대 랙 개수는 1 이상이어야 합니다.") // 최소값 1
        Integer maxRackCount, // 최대 랙 개수 (선택, null이면 기존 값 유지)

        @DecimalMin(value = "-50.0", message = "최저 온도는 -50℃ 이상이어야 합니다.") // 최소값 -50
        @DecimalMax(value = "50.0", message = "최저 온도는 50℃ 이하여야 합니다.") // 최대값 50
        @Digits(integer = 3, fraction = 2, message = "온도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.") // 정수 3자리, 소수점 2자리
        BigDecimal temperatureMin, // 최저 허용 온도 (선택, null이면 기존 값 유지)

        @DecimalMin(value = "-50.0", message = "최고 온도는 -50℃ 이상이어야 합니다.") // 최소값 -50
        @DecimalMax(value = "50.0", message = "최고 온도는 50℃ 이하여야 합니다.") // 최대값 50
        @Digits(integer = 3, fraction = 2, message = "온도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.") // 정수 3자리, 소수점 2자리
        BigDecimal temperatureMax, // 최고 허용 온도 (선택, null이면 기존 값 유지)

        @DecimalMin(value = "0.0", message = "최저 습도는 0% 이상이어야 합니다.") // 최소값 0
        @DecimalMax(value = "100.0", message = "최저 습도는 100% 이하여야 합니다.") // 최대값 100
        @Digits(integer = 3, fraction = 2, message = "습도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.") // 정수 3자리, 소수점 2자리
        BigDecimal humidityMin, // 최저 허용 습도 (선택, null이면 기존 값 유지)

        @DecimalMin(value = "0.0", message = "최고 습도는 0% 이상이어야 합니다.") // 최소값 0
        @DecimalMax(value = "100.0", message = "최고 습도는 100% 이하여야 합니다.") // 최대값 100
        @Digits(integer = 3, fraction = 2, message = "습도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.") // 정수 3자리, 소수점 2자리
        BigDecimal humidityMax, // 최고 허용 습도 (선택, null이면 기존 값 유지)

        @Min(value = 1, message = "유효하지 않은 담당자 ID입니다.") // 최소값 1
        Long managerId // 담당자 ID (선택, null이면 기존 값 유지)
) {
    // UpdateRequest는 toEntity() 메서드가 필요 없음
    // Service 계층에서 기존 Entity의 updateInfo() 메서드를 호출하여 수정
}