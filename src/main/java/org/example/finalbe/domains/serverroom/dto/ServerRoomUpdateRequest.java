package org.example.finalbe.domains.serverroom.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;

import java.math.BigDecimal;

/**
 * 서버실 수정 요청 DTO (DataCenter 필드 추가)
 */
@Builder
public record ServerRoomUpdateRequest(
        @Size(max = 100, message = "서버실 이름은 100자 이하여야 합니다.")
        String name,

        @Size(max = 50, message = "서버실 코드는 50자 이하여야 합니다.")
        String code,

        @Size(max = 255, message = "위치는 255자 이하여야 합니다.")
        String location,

        Integer floor,

        Integer rows,

        Integer columns,

        ServerRoomStatus status,

        String description,

        BigDecimal totalArea,

        BigDecimal totalPowerCapacity,

        BigDecimal totalCoolingCapacity,

        BigDecimal temperatureMin,

        BigDecimal temperatureMax,

        BigDecimal humidityMin,

        BigDecimal humidityMax,

        Long dataCenterId
) {
}