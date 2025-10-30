package org.example.finalbe.domains.datacenter.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.DataCenterStatus;

import java.math.BigDecimal;

/**
 * 전산실 수정 요청 DTO
 */
@Builder
public record DataCenterUpdateRequest(
        @Size(max = 100, message = "전산실 이름은 100자 이내로 입력해주세요.")
        String name,

        @Size(max = 50, message = "전산실 코드는 50자 이내로 입력해주세요.")
        String code,

        @Size(max = 255, message = "위치는 255자 이내로 입력해주세요.")
        String location,

        @Size(max = 50, message = "층수는 50자 이내로 입력해주세요.")
        String floor,

        @Min(value = 1, message = "행 수는 1 이상이어야 합니다.")
        Integer rows,

        @Min(value = 1, message = "열 수는 1 이상이어야 합니다.")
        Integer columns,

        @Size(max = 500, message = "배경 이미지 URL은 500자 이내로 입력해주세요.")
        String backgroundImageUrl,

        DataCenterStatus status,

        String description,

        @DecimalMin(value = "0.0", message = "면적은 0 이상이어야 합니다.")
        @Digits(integer = 10, fraction = 2, message = "면적은 정수 10자리, 소수점 2자리까지 입력 가능합니다.")
        BigDecimal totalArea,

        @DecimalMin(value = "0.0", message = "전력 용량은 0 이상이어야 합니다.")
        @Digits(integer = 10, fraction = 2, message = "전력 용량은 정수 10자리, 소수점 2자리까지 입력 가능합니다.")
        BigDecimal totalPowerCapacity,

        @DecimalMin(value = "0.0", message = "냉각 용량은 0 이상이어야 합니다.")
        @Digits(integer = 10, fraction = 2, message = "냉각 용량은 정수 10자리, 소수점 2자리까지 입력 가능합니다.")
        BigDecimal totalCoolingCapacity,

        @Min(value = 1, message = "최대 랙 개수는 1 이상이어야 합니다.")
        Integer maxRackCount,

        @DecimalMin(value = "-50.0", message = "최저 온도는 -50℃ 이상이어야 합니다.")
        @DecimalMax(value = "50.0", message = "최저 온도는 50℃ 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "온도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.")
        BigDecimal temperatureMin,

        @DecimalMin(value = "-50.0", message = "최고 온도는 -50℃ 이상이어야 합니다.")
        @DecimalMax(value = "50.0", message = "최고 온도는 50℃ 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "온도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.")
        BigDecimal temperatureMax,

        @DecimalMin(value = "0.0", message = "최저 습도는 0% 이상이어야 합니다.")
        @DecimalMax(value = "100.0", message = "최저 습도는 100% 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "습도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.")
        BigDecimal humidityMin,

        @DecimalMin(value = "0.0", message = "최고 습도는 0% 이상이어야 합니다.")
        @DecimalMax(value = "100.0", message = "최고 습도는 100% 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "습도는 정수 3자리, 소수점 2자리까지 입력 가능합니다.")
        BigDecimal humidityMax,

        @Min(value = 1, message = "유효하지 않은 담당자 ID입니다.")
        Long managerId
) {
}