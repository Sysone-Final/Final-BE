package org.example.finalbe.domains.device.dto;

import lombok.Builder;

import java.util.List;

/**
 * 서버실별 장치 목록 조회 응답 DTO
 */
@Builder
public record ServerRoomDeviceListResponse(
        ServerRoomInfo serverRoom,
        List<DeviceSimpleInfo> devices,
        Integer equipmentCount
) {
    public static ServerRoomDeviceListResponse of(ServerRoomInfo serverRoom, List<DeviceSimpleInfo> devices, Integer equipmentCount) {
        return ServerRoomDeviceListResponse.builder()
                .serverRoom(serverRoom)
                .devices(devices)
                .equipmentCount(equipmentCount)
                .build();
    }
}