package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.example.finalbe.domains.prometheus.dto.PrometheusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final NetworkMetricRepository networkMetricRepository;

    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        log.debug("📡 [Network] 메트릭 수집 시작: {} 개 장비", dataMap.size());

        collectNetworkBytes(dataMap);
        collectNetworkPackets(dataMap);
        collectNetworkErrors(dataMap);

        log.debug("✅ [Network] 메트릭 수집 완료");
    }

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
                if (data != null) {
                    setter.accept(data, value);
                }
            }
        }
    }

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
                if (data != null) {
                    setter.accept(data, value.longValue());
                }
            }
        }
    }

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
                if (data != null) {
                    setter.accept(data, value.intValue());
                }
            }
        }
    }

    private MetricRawData findDataByInstance(Map<Long, MetricRawData> dataMap, String instance) {
        return dataMap.values().stream()
                .filter(d -> instance.equals(d.getInstance()))
                .findFirst()
                .orElse(null);
    }

    /**
     * ✅ MetricRawData → NetworkMetric 변환 (Equipment 정보 포함)
     *
     * @param data MetricRawData
     * @param generateTime 생성 시간
     * @param equipment 장비 정보 (네트워크 대역폭 조회용)
     * @return NetworkMetric
     */
    public NetworkMetric convertToNetworkMetric(MetricRawData data, LocalDateTime generateTime, Equipment equipment) {
        if (data.getNetworkRxBps() == null && data.getNetworkTxBps() == null) {
            return null;
        }

        LocalDateTime finalGenerateTime = generateTime != null
                ? generateTime
                : LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault());

        // ✅ Equipment에서 네트워크 대역폭 조회 (null이면 기본값 1000Mbps)
        int bandwidthMbps = equipment != null ? equipment.getNetworkBandwidthMbpsOrDefault() : 1000;
        double bandwidthBps = bandwidthMbps * 1_000_000.0;  // Mbps → bps 변환

        // ✅ 대역폭 기반으로 사용률 계산
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

    private void updateExisting(NetworkMetric existing, NetworkMetric newMetric) {
        if (newMetric.getRxUsage() != null) existing.setRxUsage(newMetric.getRxUsage());
        if (newMetric.getTxUsage() != null) existing.setTxUsage(newMetric.getTxUsage());
        if (newMetric.getInPktsTot() != null) existing.setInPktsTot(newMetric.getInPktsTot());
        if (newMetric.getOutPktsTot() != null) existing.setOutPktsTot(newMetric.getOutPktsTot());
        if (newMetric.getInBytesTot() != null) existing.setInBytesTot(newMetric.getInBytesTot());
        if (newMetric.getOutBytesTot() != null) existing.setOutBytesTot(newMetric.getOutBytesTot());
        if (newMetric.getInBytesPerSec() != null) existing.setInBytesPerSec(newMetric.getInBytesPerSec());
        if (newMetric.getOutBytesPerSec() != null) existing.setOutBytesPerSec(newMetric.getOutBytesPerSec());
        if (newMetric.getInPktsPerSec() != null) existing.setInPktsPerSec(newMetric.getInPktsPerSec());
        if (newMetric.getOutPktsPerSec() != null) existing.setOutPktsPerSec(newMetric.getOutPktsPerSec());
        if (newMetric.getInErrorPktsTot() != null) existing.setInErrorPktsTot(newMetric.getInErrorPktsTot());
        if (newMetric.getOutErrorPktsTot() != null) existing.setOutErrorPktsTot(newMetric.getOutErrorPktsTot());
        if (newMetric.getInDiscardPktsTot() != null) existing.setInDiscardPktsTot(newMetric.getInDiscardPktsTot());
        if (newMetric.getOutDiscardPktsTot() != null) existing.setOutDiscardPktsTot(newMetric.getOutDiscardPktsTot());
        if (newMetric.getOperStatus() != null) existing.setOperStatus(newMetric.getOperStatus());
    }
}