package org.example.finalbe.domains.prometheus.dto.network;

public record NetworkInterfaceStatusResponse(
        String device,
        Integer operStatus,
        String statusText
) {
    public static NetworkInterfaceStatusResponse from(Object[] row) {
        String deviceName = (String) row[0];
        Integer operStatus = row[1] != null ? ((Number) row[1]).intValue() : 0;
        String statusText = (operStatus == 1) ? "UP" : "DOWN";

        return new NetworkInterfaceStatusResponse(deviceName, operStatus, statusText);
    }
}