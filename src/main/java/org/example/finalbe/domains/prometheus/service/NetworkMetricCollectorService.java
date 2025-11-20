package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String DEVICE_FILTER = "{device!~\"lo|veth.*|docker.*|br-.*\"}";

    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        collectNetworkBandwidth(dataMap);
        collectNetworkPackets(dataMap);
        collectNetworkErrors(dataMap);
        collectNetworkStatus(dataMap);
    }

    private void collectNetworkBandwidth(Map<Long, MetricRawData> dataMap) {
        String rxQuery = "sum by (instance) (rate(node_network_receive_bytes_total" + DEVICE_FILTER + "[5s]))";
        collectNetworkMetric(dataMap, rxQuery, MetricRawData::setNetworkRxBps);

        String txQuery = "sum by (instance) (rate(node_network_transmit_bytes_total" + DEVICE_FILTER + "[5s]))";
        collectNetworkMetric(dataMap, txQuery, MetricRawData::setNetworkTxBps);

        String rxTotalQuery = "sum by (instance) (node_network_receive_bytes_total" + DEVICE_FILTER + ")";
        collectNetworkLongMetric(dataMap, rxTotalQuery, MetricRawData::setNetworkRxBytesTotal);

        String txTotalQuery = "sum by (instance) (node_network_transmit_bytes_total" + DEVICE_FILTER + ")";
        collectNetworkLongMetric(dataMap, txTotalQuery, MetricRawData::setNetworkTxBytesTotal);
    }

    private void collectNetworkPackets(Map<Long, MetricRawData> dataMap) {
        String rxPpsQuery = "sum by (instance) (rate(node_network_receive_packets_total" + DEVICE_FILTER + "[5s]))";
        collectNetworkMetric(dataMap, rxPpsQuery, MetricRawData::setNetworkRxPps);

        String txPpsQuery = "sum by (instance) (rate(node_network_transmit_packets_total" + DEVICE_FILTER + "[5s]))";
        collectNetworkMetric(dataMap, txPpsQuery, MetricRawData::setNetworkTxPps);

        String rxPktsTotalQuery = "sum by (instance) (node_network_receive_packets_total" + DEVICE_FILTER + ")";
        collectNetworkLongMetric(dataMap, rxPktsTotalQuery, MetricRawData::setNetworkRxPacketsTotal);

        String txPktsTotalQuery = "sum by (instance) (node_network_transmit_packets_total" + DEVICE_FILTER + ")";
        collectNetworkLongMetric(dataMap, txPktsTotalQuery, MetricRawData::setNetworkTxPacketsTotal);
    }

    private void collectNetworkErrors(Map<Long, MetricRawData> dataMap) {
        String rxErrorsQuery = "sum by (instance) (node_network_receive_errs_total" + DEVICE_FILTER + ")";
        collectNetworkLongMetric(dataMap, rxErrorsQuery, MetricRawData::setNetworkRxErrors);

        String txErrorsQuery = "sum by (instance) (node_network_transmit_errs_total" + DEVICE_FILTER + ")";
        collectNetworkLongMetric(dataMap, txErrorsQuery, MetricRawData::setNetworkTxErrors);

        String rxDropsQuery = "sum by (instance) (node_network_receive_drop_total" + DEVICE_FILTER + ")";
        collectNetworkLongMetric(dataMap, rxDropsQuery, MetricRawData::setNetworkRxDrops);

        String txDropsQuery = "sum by (instance) (node_network_transmit_drop_total" + DEVICE_FILTER + ")";
        collectNetworkLongMetric(dataMap, txDropsQuery, MetricRawData::setNetworkTxDrops);
    }

    private void collectNetworkStatus(Map<Long, MetricRawData> dataMap) {
        String statusQuery = "max by (instance) (node_network_up" + DEVICE_FILTER + ")";
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(statusQuery);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setNetworkOperStatus(value.intValue());
                }
            }
        }
    }

    private void collectNetworkMetric(Map<Long, MetricRawData> dataMap, String query,
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

    private void collectNetworkLongMetric(Map<Long, MetricRawData> dataMap, String query,
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

    private MetricRawData findDataByInstance(Map<Long, MetricRawData> dataMap, String instance) {
        return dataMap.values().stream()
                .filter(d -> instance.equals(d.getInstance()))
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void saveMetrics(List<MetricRawData> dataList) {
        for (MetricRawData data : dataList) {
            try {
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

                log.debug("  ✓ NetworkMetric 저장: equipmentId={}", data.getEquipmentId());

            } catch (Exception e) {
                log.error("❌ NetworkMetric 저장 실패: equipmentId={} - {}",
                        data.getEquipmentId(), e.getMessage());
            }
        }
    }

    private NetworkMetric convertToEntity(MetricRawData data) {
        LocalDateTime generateTime = data.getTimestamp() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();

        return NetworkMetric.builder()
                .equipmentId(data.getEquipmentId())
                .nicName("aggregated")
                .generateTime(generateTime)
                .inBytesPerSec(data.getNetworkRxBps())
                .outBytesPerSec(data.getNetworkTxBps())
                .inPktsPerSec(data.getNetworkRxPps())
                .outPktsPerSec(data.getNetworkTxPps())
                .inBytesTot(data.getNetworkRxBytesTotal())
                .outBytesTot(data.getNetworkTxBytesTotal())
                .inPktsTot(data.getNetworkRxPacketsTotal())
                .outPktsTot(data.getNetworkTxPacketsTotal())
                .inErrorPktsTot(data.getNetworkRxErrors())
                .outErrorPktsTot(data.getNetworkTxErrors())
                .inDiscardPktsTot(data.getNetworkRxDrops())
                .outDiscardPktsTot(data.getNetworkTxDrops())
                .operStatus(data.getNetworkOperStatus())
                .build();
    }

    private void updateExisting(NetworkMetric existing, NetworkMetric newMetric) {
        existing.setInBytesPerSec(newMetric.getInBytesPerSec());
        existing.setOutBytesPerSec(newMetric.getOutBytesPerSec());
        existing.setInPktsPerSec(newMetric.getInPktsPerSec());
        existing.setOutPktsPerSec(newMetric.getOutPktsPerSec());
        existing.setInBytesTot(newMetric.getInBytesTot());
        existing.setOutBytesTot(newMetric.getOutBytesTot());
        existing.setInPktsTot(newMetric.getInPktsTot());
        existing.setOutPktsTot(newMetric.getOutPktsTot());
        existing.setInErrorPktsTot(newMetric.getInErrorPktsTot());
        existing.setOutErrorPktsTot(newMetric.getOutErrorPktsTot());
        existing.setInDiscardPktsTot(newMetric.getInDiscardPktsTot());
        existing.setOutDiscardPktsTot(newMetric.getOutDiscardPktsTot());
        existing.setOperStatus(newMetric.getOperStatus());
    }
}