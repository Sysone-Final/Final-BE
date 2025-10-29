package org.example.finalbe.domains.datacenter.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.member.domain.Member;

import java.math.BigDecimal;

/**
 * 전산실 생성 요청 DTO
 *
 * - Record: 불변 객체로 DTO를 정의 (Java 14+)
 * - Bean Validation: @NotBlank, @NotNull 등으로 입력값 검증
 * - Builder 패턴: 가독성 높은 객체 생성 지원
 * - toEntity(): DTO → Entity 변환 메서드
 */
@Builder // 빌더 패턴을 사용하여 가독성 높은 객체 생성
public record DataCenterCreateRequest(
        @NotBlank(message = "전산실 이름을 입력해주세요.") // null, 빈 문자열, 공백만 있는 문자열 모두 거부
        @Size(max = 100, message = "전산실 이름은 100자 이내로 입력해주세요.") // 최대 100자 제한
        String name, // 전산실 이름 (필수)

        @NotBlank(message = "전산실 코드를 입력해주세요.") // null, 빈 문자열, 공백만 있는 문자열 모두 거부
        @Size(max = 50, message = "전산실 코드는 50자 이내로 입력해주세요.") // 최대 50자 제한
        String code, // 전산실 코드 (필수, UNIQUE)

        @Size(max = 255, message = "위치는 255자 이내로 입력해주세요.") // 최대 255자 제한
        String location, // 전산실 위치/주소 (선택)

        @Size(max = 50, message = "층수는 50자 이내로 입력해주세요.") // 최대 50자 제한
        String floor, // 전산실 층수 (선택)

        @Min(value = 1, message = "행 수는 1 이상이어야 합니다.") // 최소값 1
        Integer rows, // 랙 배치 행 수 (선택, 1 이상)

        @Min(value = 1, message = "열 수는 1 이상이어야 합니다.") // 최소값 1
        Integer columns, // 랙 배치 열 수 (선택, 1 이상)

        @Size(max = 500, message = "배경 이미지 URL은 500자 이내로 입력해주세요.") // 최대 500자 제한
        String backgroundImageUrl, // 배경 이미지 URL (선택)

        DataCenterStatus status, // 전산실 상태 (선택, 미입력 시 ACTIVE 기본값)

        String description, // 전산실 설명 (선택, TEXT 타입이므로 길이 제한 없음)

        @DecimalMin(value = "0.0", message = "면적은 0 이상이어야 합니다.") // 최소값 0
        @Digits(integer = 10, fraction = 2, message = "면적은 정수 10자리, 소수점 2자리까지 입력 가능합니다.") // 정수 10자리, 소수점 2자리
        BigDecimal totalArea, // 총 면적 (선택, m²)

        @DecimalMin(value = "0.0", message = "전력 용량은 0 이상이어야 합니다.") // 최소값 0
        @Digits(integer = 10, fraction = 2, message = "전력 용량은 정수 10자리, 소수점 2자리까지 입력 가능합니다.") // 정수 10자리, 소수점 2자리
        BigDecimal totalPowerCapacity, // 총 전력 용량 (선택, kW)

        @DecimalMin(value = "0.0", message = "냉각 용량은 0 이상이어야 합니다.") // 최소값 0
        @Digits(integer = 10, fraction = 2, message = "냉각 용량은 정수 10자리, 소수점 2자리까지 입력 가능합니다.") // 정수 10자리, 소수점 2자리
        BigDecimal totalCoolingCapacity, // 총 냉각 용량 (선택, kW)

        @Min(value = 1, message = "최대 랙 개수는 1 이상이어야 합니다.") // 최소값 1
        Integer maxRackCount, // 최대 랙 개수 (선택, 1 이상)

        @DecimalMin(value = "-50.0", message = "최저 온도는 -50℃ 이상이어야 합니다.") // 최소값 -50 (극한 환경 고려)
        @DecimalMax(value = "50.0", message = "최저 온도는 50℃ 이하여야 합니다.") // 최대값 50
        @Digits(integer = 3, fraction = 2, message = "온도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.") // 정수 3자리, 소수점 2자리
        BigDecimal temperatureMin, // 최저 허용 온도 (선택, ℃)

        @DecimalMin(value = "-50.0", message = "최고 온도는 -50℃ 이상이어야 합니다.") // 최소값 -50
        @DecimalMax(value = "50.0", message = "최고 온도는 50℃ 이하여야 합니다.") // 최대값 50
        @Digits(integer = 3, fraction = 2, message = "온도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.") // 정수 3자리, 소수점 2자리
        BigDecimal temperatureMax, // 최고 허용 온도 (선택, ℃)

        @DecimalMin(value = "0.0", message = "최저 습도는 0% 이상이어야 합니다.") // 최소값 0
        @DecimalMax(value = "100.0", message = "최저 습도는 100% 이하여야 합니다.") // 최대값 100
        @Digits(integer = 3, fraction = 2, message = "습도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.") // 정수 3자리, 소수점 2자리
        BigDecimal humidityMin, // 최저 허용 습도 (선택, %)

        @DecimalMin(value = "0.0", message = "최고 습도는 0% 이상이어야 합니다.") // 최소값 0
        @DecimalMax(value = "100.0", message = "최고 습도는 100% 이하여야 합니다.") // 최대값 100
        @Digits(integer = 3, fraction = 2, message = "습도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.") // 정수 3자리, 소수점 2자리
        BigDecimal humidityMax, // 최고 허용 습도 (선택, %)

        @NotNull(message = "담당자를 지정해주세요.") // null 거부
        @Min(value = 1, message = "유효하지 않은 담당자 ID입니다.") // 최소값 1
        Long managerId // 담당자 ID (필수)
) {
    /**
     * DTO를 Entity로 변환
     * Service 계층에서 호출하여 DataCenter 엔티티 생성
     *
     * @param manager 담당자 엔티티 (Repository에서 조회)
     * @param createdBy 생성자 이름 (현재 로그인한 사용자)
     * @return DataCenter 엔티티
     */
    public DataCenter toEntity(Member manager, String createdBy) {
        return DataCenter.builder()
                .name(this.name) // 전산실 이름
                .code(this.code) // 전산실 코드
                .location(this.location) // 위치/주소
                .floor(this.floor) // 층수
                .rows(this.rows) // 행 수
                .columns(this.columns) // 열 수
                .backgroundImageUrl(this.backgroundImageUrl) // 배경 이미지 URL
                .status(this.status != null ? this.status : DataCenterStatus.ACTIVE) // 상태 (null이면 ACTIVE 기본값)
                .description(this.description) // 설명
                .totalArea(this.totalArea) // 총 면적
                .totalPowerCapacity(this.totalPowerCapacity) // 총 전력 용량
                .totalCoolingCapacity(this.totalCoolingCapacity) // 총 냉각 용량
                .maxRackCount(this.maxRackCount) // 최대 랙 개수
                .currentRackCount(0) // 초기 랙 개수는 0으로 설정
                .temperatureMin(this.temperatureMin) // 최저 온도
                .temperatureMax(this.temperatureMax) // 최고 온도
                .humidityMin(this.humidityMin) // 최저 습도
                .humidityMax(this.humidityMax) // 최고 습도
                .manager(manager) // 담당자 (ManyToOne 관계)
                .build(); // Builder 패턴으로 객체 생성
    }
}