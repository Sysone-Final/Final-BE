/**
 * 작성자: 황요한
 * 네트워크 관련 Prometheus 메트릭을 수집하여 RawData에 반영하는 서비스
 */
package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.example.finalbe.domains.prometheus.dto.PrometheusResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkMetricCollectorService {

    private final PrometheusQueryService prometheusQuery;

    // 네트워크 메트릭 전체 수집
    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        log.debug("📡 [Network] 메트릭 수집 시작: {} 개 장비", dataMap.size());

        collectNetworkBytes(dataMap);
        collectNetworkPackets(dataMap);
        collectNetworkErrors(dataMap);

        log.debug("✅ [Network] 메트릭 수집 완료");
    }

    // 네트워크 바이트 관련 메트릭 수집
    private void collectNetworkBytes(Map<Long, MetricRawData> dataMap) {
        String rxBpsQuery = "sum by (instance) (rate(node_network_receive_bytes_total[15s]))";
        String txBpsQuery = "sum by (instance) (rate(node_network_transmit_bytes_total[15s]))";

        collectMetricAndSetDouble(dataMap, rxBpsQuery, MetricRawData::setNetworkRxBps);
        collectMetricAndSetDouble(dataMap, txBpsQuery, MetricRawData::setNetworkTxBps);

        String rxTotalQuery = "sum by (instance) (node_network_receive_bytes_total)";
        String txTotalQuery = "sum by (instance) (node_network_transmit_bytes_total)";

        collectMetricAndSetLong(dataMap, rxTotalQuery, MetricRawData::setNetworkRxBytesTotal);
        collectMetricAndSetLong(dataMap, txTotalQuery, MetricRawData::setNetworkTxBytesTotal);
    }

    // 네트워크 패킷 관련 메트릭 수집
    private void collectNetworkPackets(Map<Long, MetricRawData> dataMap) {
        String rxPpsQuery = "sum by (instance) (rate(node_network_receive_packets_total[15s]))";
        String txPpsQuery = "sum by (instance) (rate(node_network_transmit_packets_total[15s]))";

        collectMetricAndSetDouble(dataMap, rxPpsQuery, MetricRawData::setNetworkRxPps);
        collectMetricAndSetDouble(dataMap, txPpsQuery, MetricRawData::setNetworkTxPps);

        String rxPktsTotalQuery = "sum by (instance) (node_network_receive_packets_total)";
        String txPktsTotalQuery = "sum by (instance) (node_network_transmit_packets_total)";

        collectMetricAndSetLong(dataMap, rxPktsTotalQuery, MetricRawData::setNetworkRxPacketsTotal);
        collectMetricAndSetLong(dataMap, txPktsTotalQuery, MetricRawData::setNetworkTxPacketsTotal);
    }

    // 네트워크 에러/드롭 패킷 관련 메트릭 수집
    private void collectNetworkErrors(Map<Long, MetricRawData> dataMap) {
        String inErrQuery = "sum by (instance) (node_network_receive_errs_total)";
        String outErrQuery = "sum by (instance) (node_network_transmit_errs_total)";
        String inDropQuery = "sum by (instance) (node_network_receive_drop_total)";
        String outDropQuery = "sum by (instance) (node_network_transmit_drop_total)";

        collectMetricAndSetLong(dataMap, inErrQuery, MetricRawData::setNetworkRxErrors);
        collectMetricAndSetLong(dataMap, outErrQuery, MetricRawData::setNetworkTxErrors);
        collectMetricAndSetLong(dataMap, inDropQuery, MetricRawData::setNetworkRxDrops);
        collectMetricAndSetLong(dataMap, outDropQuery, MetricRawData::setNetworkTxDrops);

        String upQuery = "max by (instance) (node_network_up)";
        collectMetricAndSetInteger(dataMap, upQuery, MetricRawData::setNetworkOperStatus);
    }

    // 실수(Double) 메트릭 수집 및 반영
    private void collectMetricAndSetDouble(
            Map<Long, MetricRawData> dataMap,
            String query,
            java.util.function.BiConsumer<MetricRawData, Double> setter) {

        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) setter.accept(data, value);
            }
        }
    }

    // 정수(Long) 메트릭 수집 및 반영
    private void collectMetricAndSetLong(
            Map<Long, MetricRawData> dataMap,
            String query,
            java.util.function.BiConsumer<MetricRawData, Long> setter) {

        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) setter.accept(data, value.longValue());
            }
        }
    }

    // 정수(Integer) 메트릭 수집 및 반영
    private void collectMetricAndSetInteger(
            Map<Long, MetricRawData> dataMap,
            String query,
            java.util.function.BiConsumer<MetricRawData, Integer> setter) {

        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) setter.accept(data, value.intValue());
            }
        }
    }

    // instance로 RawData 조회
    private MetricRawData findDataByInstance(Map<Long, MetricRawData> dataMap, String instance) {
        return dataMap.values().stream()
                .filter(d -> instance.equals(d.getInstance()))
                .findFirst()
                .orElse(null);
    }

    // MetricRawData → NetworkMetric 변환
    public NetworkMetric convertToNetworkMetric(MetricRawData data, LocalDateTime generateTime, Equipment equipment) {
        if (data.getNetworkRxBps() == null && data.getNetworkTxBps() == null) return null;

        LocalDateTime finalGenerateTime = generateTime != null
                ? generateTime
                : LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault());

        int bandwidthMbps = equipment != null ? equipment.getNetworkBandwidthMbpsOrDefault() : 1000;
        double bandwidthBps = bandwidthMbps * 1_000_000.0;

        Double rxUsage = data.getNetworkRxBps() != null
                ? (data.getNetworkRxBps() / bandwidthBps) * 100
                : null;

        Double txUsage = data.getNetworkTxBps() != null
                ? (data.getNetworkTxBps() / bandwidthBps) * 100
                : null;

        return NetworkMetric.builder()
                .equipmentId(data.getEquipmentId())
                .nicName("aggregated")
                .generateTime(finalGenerateTime)
                .rxUsage(rxUsage)
                .txUsage(txUsage)
                .inPktsTot(data.getNetworkRxPacketsTotal())
                .outPktsTot(data.getNetworkTxPacketsTotal())
                .inBytesTot(data.getNetworkRxBytesTotal())
                .outBytesTot(data.getNetworkTxBytesTotal())
                .inBytesPerSec(data.getNetworkRxBps())
                .outBytesPerSec(data.getNetworkTxBps())
                .inPktsPerSec(data.getNetworkRxPps())
                .outPktsPerSec(data.getNetworkTxPps())
                .inErrorPktsTot(data.getNetworkRxErrors())
                .outErrorPktsTot(data.getNetworkTxErrors())
                .inDiscardPktsTot(data.getNetworkRxDrops())
                .outDiscardPktsTot(data.getNetworkTxDrops())
                .operStatus(data.getNetworkOperStatus())
                .build();
    }
}
