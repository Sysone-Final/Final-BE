package org.example.finalbe.domains.prometheus.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.prometheus.dto.PrometheusQueryResponse;  // 변경
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
    public PrometheusQueryResponse query(String promQL) {  // 변경
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(prometheusUrl + "/api/v1/query")
                    .queryParam("query", promQL)
                    .toUriString();

            log.debug("Prometheus Query: {}", promQL);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return parseResponse(response.getBody());

        } catch (Exception e) {
            log.error("Prometheus query 실패: {}", promQL, e);
            return PrometheusQueryResponse.empty();  // 변경
        }
    }

    /**
     * 범위 쿼리 (/api/v1/query_range)
     */
    public PrometheusQueryResponse queryRange(String promQL, Instant start, Instant end, String step) {  // 변경
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(prometheusUrl + "/api/v1/query_range")
                    .queryParam("query", promQL)
                    .queryParam("start", start.getEpochSecond())
                    .queryParam("end", end.getEpochSecond())
                    .queryParam("step", step)
                    .toUriString();

            log.debug("Prometheus Query Range: {} ({}~{})", promQL, start, end);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return parseResponse(response.getBody());

        } catch (Exception e) {
            log.error("Prometheus query_range 실패: {}", promQL, e);
            return PrometheusQueryResponse.empty();  // 변경
        }
    }

    /**
     * 응답 파싱
     */
    private PrometheusQueryResponse parseResponse(String json) {  // 변경
        try {
            JsonNode root = objectMapper.readTree(json);

            if (!"success".equals(root.path("status").asText())) {
                log.warn("Prometheus 응답 실패: {}", json);
                return PrometheusQueryResponse.empty();  // 변경
            }

            JsonNode data = root.path("data");
            String resultType = data.path("resultType").asText();
            JsonNode results = data.path("result");

            List<PrometheusQueryResponse.Result> parsedResults = new ArrayList<>();  // 변경

            for (JsonNode result : results) {
                JsonNode metric = result.path("metric");

                // vector 타입
                if ("vector".equals(resultType)) {
                    JsonNode value = result.path("value");
                    if (value.isArray() && value.size() == 2) {
                        parsedResults.add(new PrometheusQueryResponse.Result(  // 변경
                                objectMapper.convertValue(metric, java.util.Map.class),
                                value.get(0).asLong(),
                                parseValue(value.get(1))
                        ));
                    }
                }
                // matrix 타입 (query_range)
                else if ("matrix".equals(resultType)) {
                    JsonNode values = result.path("values");
                    for (JsonNode value : values) {
                        if (value.isArray() && value.size() == 2) {
                            parsedResults.add(new PrometheusQueryResponse.Result(  // 변경
                                    objectMapper.convertValue(metric, java.util.Map.class),
                                    value.get(0).asLong(),
                                    parseValue(value.get(1))
                            ));
                        }
                    }
                }
            }

            return new PrometheusQueryResponse("success", resultType, parsedResults);  // 변경

        } catch (Exception e) {
            log.error("Prometheus 응답 파싱 실패", e);
            return PrometheusQueryResponse.empty();  // 변경
        }
    }

    /**
     * 값 파싱 (문자열 또는 숫자)
     */
    private Double parseValue(JsonNode node) {
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