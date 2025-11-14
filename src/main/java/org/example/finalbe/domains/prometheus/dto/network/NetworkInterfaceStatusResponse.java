package org.example.finalbe.domains.prometheus.dto.network;

/**
 * 네트워크 인터페이스 상태 응답 DTO
 * 그래프 3.9: 인터페이스 상태 패널
 */
public record NetworkInterfaceStatusResponse(
        String device,
        Integer operStatus,
        String statusText
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String device;
        private Integer operStatus;
        private String statusText;

        public Builder device(String device) {
            this.device = device;
            return this;
        }

        public Builder operStatus(Integer operStatus) {
            this.operStatus = operStatus;
            return this;
        }

        public Builder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public NetworkInterfaceStatusResponse build() {
            return new NetworkInterfaceStatusResponse(device, operStatus, statusText);
        }
    }
}