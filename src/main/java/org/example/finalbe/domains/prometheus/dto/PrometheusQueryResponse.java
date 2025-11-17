package org.example.finalbe.domains.prometheus.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record PrometheusQueryResponse(
        String status,
        String resultType,
        List<Result> results
) {
    public record Result(
            Map<String, String> metric,
            Long timestamp,
            Double value
    ) {
        public String getInstance() {
            return metric != null ? metric.getOrDefault("instance", "unknown") : "unknown";
        }

        public String getDevice() {
            return metric != null ? metric.get("device") : null;
        }

        public String getMountpoint() {
            return metric != null ? metric.get("mountpoint") : null;
        }

        public String getMode() {
            return metric != null ? metric.get("mode") : null;
        }

        public String getFstype() {
            return metric != null ? metric.get("fstype") : null;
        }

        public String getCpu() {
            return metric != null ? metric.get("cpu") : null;
        }
    }

    public static PrometheusQueryResponse empty() {
        return new PrometheusQueryResponse("error", null, new ArrayList<>());
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
}