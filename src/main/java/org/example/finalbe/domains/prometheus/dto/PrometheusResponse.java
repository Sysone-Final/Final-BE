package org.example.finalbe.domains.prometheus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrometheusResponse(
        String status,
        PrometheusData data,
        String errorType,
        String error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrometheusData(
            String resultType,
            List<PrometheusResult> result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrometheusResult(
            Map<String, String> metric,
            List<Object> value
    ) {
        public String getInstance() {
            return metric != null ? metric.get("instance") : null;
        }

        public String getMode() {
            return metric != null ? metric.get("mode") : null;
        }

        public String getDevice() {
            return metric != null ? metric.get("device") : null;
        }

        public String getCpu() {
            return metric != null ? metric.get("cpu") : null;
        }

        public Double getValue() {
            if (value != null && value.size() > 1) {
                Object val = value.get(1);
                if (val instanceof String) {
                    try {
                        return Double.parseDouble((String) val);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
                return ((Number) val).doubleValue();
            }
            return null;
        }

        public Long getTimestamp() {
            if (value != null && !value.isEmpty()) {
                return ((Number) value.get(0)).longValue();
            }
            return null;
        }
    }
}