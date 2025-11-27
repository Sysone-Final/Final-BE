/**
 * 작성자: 황요한
 * 서버실 생성 요청 DTO
 */
package org.example.finalbe.domains.serverroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.example.finalbe.domains.common.enumdir.ServerRoomStatus;
import org.example.finalbe.domains.serverroom.domain.ServerRoom;

import java.math.BigDecimal;

@Builder
public record ServerRoomCreateRequest(
        @NotBlank @Size(max = 100) String name,            // 이름
        @Size(max = 50) String code,                       // 코드
        @Size(max = 255) String location,                  // 위치
        Integer floor,                                     // 층수
        Integer rows,                                      // 그리드 행
        Integer columns,                                   // 그리드 열
        ServerRoomStatus status,                           // 상태
        String description,                                // 설명
        BigDecimal totalArea,                              // 총 면적
        BigDecimal totalPowerCapacity,                     // 전력 용량
        BigDecimal totalCoolingCapacity,                   // 냉각 용량
        BigDecimal temperatureMin,                         // 온도 최소
        BigDecimal temperatureMax,                         // 온도 최대
        BigDecimal humidityMin,                            // 습도 최소
        BigDecimal humidityMax,                            // 습도 최대
        Long dataCenterId                                  // 데이터센터 ID
) {
    /** Entity 변환 */
    public ServerRoom toEntity() {
        return ServerRoom.builder()
                .name(name)
                .code(code)
                .location(location)
                .floor(floor)
                .rows(rows)
                .columns(columns)
                .status(status != null ? status : ServerRoomStatus.ACTIVE)
                .description(description)
                .totalArea(totalArea)
                .totalPowerCapacity(totalPowerCapacity)
                .totalCoolingCapacity(totalCoolingCapacity)
                .temperatureMin(temperatureMin)
                .temperatureMax(temperatureMax)
                .humidityMin(humidityMin)
                .humidityMax(humidityMax)
                .build();
    }
}
