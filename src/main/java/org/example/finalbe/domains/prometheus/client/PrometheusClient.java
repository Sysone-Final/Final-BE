package org.example.finalbe.domains.prometheus.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.PrometheusQueryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${prometheus.server.url:http://112.221.184.61:9090}")
    private String prometheusUrl;

    /**
     * 즉시 쿼리 (/api/v1/query)
     */
    public PrometheusQueryResponse query(String promQL) {
        try {
            // ✅ UriComponentsBuilder로 URL 인코딩
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(prometheusUrl + "/api/v1/query")
                    .queryParam("query", promQL)
                    .build(true)  // ✨ 이미 인코딩된 값 사용
                    .toUri();

            log.debug("Prometheus Query: {}", promQL);
            log.debug("Request URL: {}", uri);

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return parseResponse(response.getBody());

        } catch (Exception e) {
            log.error("Prometheus query 실패: {}", promQL, e);
            return PrometheusQueryResponse.empty();
        }
    }

    /**
     * 범위 쿼리 (/api/v1/query_range) - ✅ 수정됨
     */
    public PrometheusQueryResponse queryRange(String promQL, Instant start, Instant end, String step) {
        try {
            // ✅ UriComponentsBuilder로 URL 인코딩 (build() 대신 build(false) 사용)
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(prometheusUrl + "/api/v1/query_range")
                    .queryParam("query", promQL)
                    .queryParam("start", start.getEpochSecond())
                    .queryParam("end", end.getEpochSecond())
                    .queryParam("step", step)
                    .build(false)  // ✨ URL 인코딩 수행
                    .toUri();

            log.debug("Prometheus Query Range: {} ({}~{})", promQL, start, end);
            log.debug("Request URL: {}", uri);

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return parseResponse(response.getBody());

        } catch (Exception e) {
            log.error("Prometheus query_range 실패: {}", promQL, e);
            return PrometheusQueryResponse.empty();
        }
    }

    /**
     * 응답 파싱 - ✅ Null 체크 강화
     */
    private PrometheusQueryResponse parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            if (!"success".equals(root.path("status").asText())) {
                log.warn("Prometheus 응답 실패: {}", json);
                return PrometheusQueryResponse.empty();
            }

            JsonNode data = root.path("data");
            String resultType = data.path("resultType").asText();
            JsonNode results = data.path("result");

            // ✅ Null 체크 추가
            if (results == null || !results.isArray()) {
                log.warn("Prometheus 결과 없음 또는 잘못된 형식");
                return PrometheusQueryResponse.empty();
            }

            List<PrometheusQueryResponse.Result> parsedResults = new ArrayList<>();

            for (JsonNode result : results) {
                JsonNode metric = result.path("metric");

                // ✅ Null 체크
                if (metric == null || metric.isMissingNode()) {
                    continue;
                }

                // vector 타입
                if ("vector".equals(resultType)) {
                    JsonNode value = result.path("value");
                    if (value.isArray() && value.size() == 2) {
                        parsedResults.add(new PrometheusQueryResponse.Result(
                                objectMapper.convertValue(metric, java.util.Map.class),
                                value.get(0).asLong(),
                                parseValue(value.get(1))
                        ));
                    }
                }
                // matrix 타입 (query_range)
                else if ("matrix".equals(resultType)) {
                    JsonNode values = result.path("values");
                    if (values != null && values.isArray()) {
                        for (JsonNode value : values) {
                            if (value.isArray() && value.size() == 2) {
                                parsedResults.add(new PrometheusQueryResponse.Result(
                                        objectMapper.convertValue(metric, java.util.Map.class),
                                        value.get(0).asLong(),
                                        parseValue(value.get(1))
                                ));
                            }
                        }
                    }
                }
            }

            log.debug("파싱된 결과 수: {}", parsedResults.size());
            return new PrometheusQueryResponse("success", resultType, parsedResults);

        } catch (Exception e) {
            log.error("Prometheus 응답 파싱 실패", e);
            return PrometheusQueryResponse.empty();
        }
    }

    /**
     * 값 파싱 (문자열 또는 숫자)
     */
    private Double parseValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0.0;
        }
        if (node.isNumber()) {
            return node.asDouble();
        } else if (node.isTextual()) {
            try {
                return Double.parseDouble(node.asText());
            } catch (NumberFormatException e) {
                log.warn("숫자 파싱 실패: {}", node.asText());
                return 0.0;
            }
        }
        return 0.0;
    }
}