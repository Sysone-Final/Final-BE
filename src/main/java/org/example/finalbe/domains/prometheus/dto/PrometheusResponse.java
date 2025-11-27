// 작성자: 황요한
// Prometheus API 응답 DTO (status / data / result 구조 매핑)

package org.example.finalbe.domains.prometheus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrometheusResponse(
        String status,            // 성공/실패 상태
        PrometheusData data,      // 데이터 영역
        String errorType,
        String error
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrometheusData(
            String resultType,                // vector, matrix 등
            List<PrometheusResult> result     // 실제 metric 값
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrometheusResult(
            Map<String, String> metric,       // instance, mode, device 등 라벨
            List<Object> value                // [timestamp, value]
    ) {
        public String getInstance() { return metric != null ? metric.get("instance") : null; }
        public String getMode() { return metric != null ? metric.get("mode") : null; }
        public String getDevice() { return metric != null ? metric.get("device") : null; }
        public String getCpu() { return metric != null ? metric.get("cpu") : null; }

        // 값 파싱
        public Double getValue() {
            if (value == null || value.size() < 2) return null;
            Object val = value.get(1);
            if (val instanceof String s) {
                try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
            }
            return ((Number) val).doubleValue();
        }

        // UNIX timestamp
        public Long getTimestamp() {
            if (value == null || value.isEmpty()) return null;
            return ((Number) value.get(0)).longValue();
        }
    }
}
