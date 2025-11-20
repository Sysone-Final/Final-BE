package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.example.finalbe.domains.monitoring.repository.NetworkMetricRepository;
import org.example.finalbe.domains.prometheus.dto.MetricRawData;
import org.example.finalbe.domains.prometheus.dto.PrometheusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
        collectNetworkTraffic(dataMap);
        collectNetworkPackets(dataMap);
        collectNetworkErrors(dataMap);
    }

    /**
     * ✅ 필터 완전 제거 - 모든 네트워크 인터페이스 수집
     */
    private void collectNetworkTraffic(Map<Long, MetricRawData> dataMap) {
        String rxQuery = "sum by (instance) (rate(node_network_receive_bytes_total[5s]))";
        String txQuery = "sum by (instance) (rate(node_network_transmit_bytes_total[5s]))";

        collectMetricAndSetDouble(dataMap, rxQuery, MetricRawData::setNetworkRxBps);
        collectMetricAndSetDouble(dataMap, txQuery, MetricRawData::setNetworkTxBps);

        String rxTotalQuery = "sum by (instance) (node_network_receive_bytes_total)";
        String txTotalQuery = "sum by (instance) (node_network_transmit_bytes_total)";

        collectMetricAndSetLong(dataMap, rxTotalQuery, MetricRawData::setNetworkRxBytesTotal);
        collectMetricAndSetLong(dataMap, txTotalQuery, MetricRawData::setNetworkTxBytesTotal);
    }

    private void collectNetworkPackets(Map<Long, MetricRawData> dataMap) {
        String rxPpsQuery = "sum by (instance) (rate(node_network_receive_packets_total[5s]))";
        String txPpsQuery = "sum by (instance) (rate(node_network_transmit_packets_total[5s]))";

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
     * ✅ 트랜잭션 분리
     */
    public void saveMetrics(List<MetricRawData> dataList) {
        int successCount = 0;
        int failureCount = 0;

        for (MetricRawData data : dataList) {
            try {
                saveMetricWithNewTransaction(data);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("❌ NetworkMetric 저장 실패: equipmentId={} - {}",
                        data.getEquipmentId(), e.getMessage());
            }
        }

        if (failureCount > 0) {
            log.warn("⚠️ NetworkMetric 저장 완료: 성공={}, 실패={}", successCount, failureCount);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMetricWithNewTransaction(MetricRawData data) {
        NetworkMetric metric = convertToEntity(data);

        NetworkMetric existing = networkMetricRepository
                .findByEquipmentIdAndNicNameAndGenerateTime(
                        data.getEquipmentId(), "aggregated", metric.getGenerateTime())
                .orElse(null);

        if (existing != null) {
            updateExisting(existing, metric);
            networkMetricRepository.save(existing);
        } else {
            networkMetricRepository.save(metric);
        }
    }

    /**
     * ✅ NetworkMetric 엔티티 필드명에 맞게 매핑
     */
    private NetworkMetric convertToEntity(MetricRawData data) {
        LocalDateTime generateTime = data.getTimestamp() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();

        // 대역폭 1Gbps 기준으로 사용률 계산
        double bandwidthBps = 1_000_000_000.0;  // 1Gbps
        Double rxUsage = data.getNetworkRxBps() != null
                ? (data.getNetworkRxBps() / bandwidthBps) * 100
                : null;
        Double txUsage = data.getNetworkTxBps() != null
                ? (data.getNetworkTxBps() / bandwidthBps) * 100
                : null;

        return NetworkMetric.builder()
                .equipmentId(data.getEquipmentId())
                .nicName("aggregated")
                .generateTime(generateTime)
                // 사용률 (%)
                .rxUsage(rxUsage)
                .txUsage(txUsage)
                // 패킷 누적
                .inPktsTot(data.getNetworkRxPacketsTotal())
                .outPktsTot(data.getNetworkTxPacketsTotal())
                // 바이트 누적
                .inBytesTot(data.getNetworkRxBytesTotal())
                .outBytesTot(data.getNetworkTxBytesTotal())
                // 초당 전송량
                .inBytesPerSec(data.getNetworkRxBps())
                .outBytesPerSec(data.getNetworkTxBps())
                .inPktsPerSec(data.getNetworkRxPps())
                .outPktsPerSec(data.getNetworkTxPps())
                // 에러/드롭
                .inErrorPktsTot(data.getNetworkRxErrors())
                .outErrorPktsTot(data.getNetworkTxErrors())
                .inDiscardPktsTot(data.getNetworkRxDrops())
                .outDiscardPktsTot(data.getNetworkTxDrops())
                // 인터페이스 상태
                .operStatus(data.getNetworkOperStatus())
                .build();
    }

    private void updateExisting(NetworkMetric existing, NetworkMetric newMetric) {
        // 사용률
        if (newMetric.getRxUsage() != null) existing.setRxUsage(newMetric.getRxUsage());
        if (newMetric.getTxUsage() != null) existing.setTxUsage(newMetric.getTxUsage());

        // 패킷 누적
        if (newMetric.getInPktsTot() != null) existing.setInPktsTot(newMetric.getInPktsTot());
        if (newMetric.getOutPktsTot() != null) existing.setOutPktsTot(newMetric.getOutPktsTot());

        // 바이트 누적
        if (newMetric.getInBytesTot() != null) existing.setInBytesTot(newMetric.getInBytesTot());
        if (newMetric.getOutBytesTot() != null) existing.setOutBytesTot(newMetric.getOutBytesTot());

        // 초당 전송량
        if (newMetric.getInBytesPerSec() != null) existing.setInBytesPerSec(newMetric.getInBytesPerSec());
        if (newMetric.getOutBytesPerSec() != null) existing.setOutBytesPerSec(newMetric.getOutBytesPerSec());
        if (newMetric.getInPktsPerSec() != null) existing.setInPktsPerSec(newMetric.getInPktsPerSec());
        if (newMetric.getOutPktsPerSec() != null) existing.setOutPktsPerSec(newMetric.getOutPktsPerSec());

        // 에러/드롭
        if (newMetric.getInErrorPktsTot() != null) existing.setInErrorPktsTot(newMetric.getInErrorPktsTot());
        if (newMetric.getOutErrorPktsTot() != null) existing.setOutErrorPktsTot(newMetric.getOutErrorPktsTot());
        if (newMetric.getInDiscardPktsTot() != null) existing.setInDiscardPktsTot(newMetric.getInDiscardPktsTot());
        if (newMetric.getOutDiscardPktsTot() != null) existing.setOutDiscardPktsTot(newMetric.getOutDiscardPktsTot());

        // 상태
        if (newMetric.getOperStatus() != null) existing.setOperStatus(newMetric.getOperStatus());
    }
}