package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
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
public class DiskMetricCollectorService {

    private final PrometheusQueryService prometheusQuery;
    private final DiskMetricRepository diskMetricRepository;

    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        collectDiskSpace(dataMap);
        collectDiskInodes(dataMap);
        collectDiskIO(dataMap);
    }

    /**
     * ✅ 필터 완전 제거 - 모든 파일시스템 수집
     */
    private void collectDiskSpace(Map<Long, MetricRawData> dataMap) {
        String totalQuery = "sum by (instance) (node_filesystem_size_bytes)";
        List<PrometheusResponse.PrometheusResult> totalResults = prometheusQuery.query(totalQuery);

        for (PrometheusResponse.PrometheusResult result : totalResults) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setTotalDisk(value.longValue());  // ✅ setDiskTotalBytes → setTotalDisk
                }
            }
        }

        String freeQuery = "sum by (instance) (node_filesystem_free_bytes)";
        List<PrometheusResponse.PrometheusResult> freeResults = prometheusQuery.query(freeQuery);

        for (PrometheusResponse.PrometheusResult result : freeResults) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setFreeDisk(value.longValue());  // ✅ setDiskFreeBytes → setFreeDisk
                    if (data.getTotalDisk() != null) {    // ✅ getDiskTotalBytes → getTotalDisk
                        data.setUsedDisk(data.getTotalDisk() - value.longValue());  // ✅ setDiskUsedBytes → setUsedDisk
                    }
                }
            }
        }
    }

    private void collectDiskInodes(Map<Long, MetricRawData> dataMap) {
        String totalQuery = "sum by (instance) (node_filesystem_files)";
        List<PrometheusResponse.PrometheusResult> totalResults = prometheusQuery.query(totalQuery);

        for (PrometheusResponse.PrometheusResult result : totalResults) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setTotalInodes(value.longValue());  // ✅ setDiskTotalInodes → setTotalInodes
                }
            }
        }

        String freeQuery = "sum by (instance) (node_filesystem_files_free)";
        List<PrometheusResponse.PrometheusResult> freeResults = prometheusQuery.query(freeQuery);

        for (PrometheusResponse.PrometheusResult result : freeResults) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setFreeInodes(value.longValue());  // ✅ setDiskFreeInodes → setFreeInodes
                }
            }
        }
    }

    private void collectDiskIO(Map<Long, MetricRawData> dataMap) {
        String readQuery = "sum by (instance) (rate(node_disk_read_bytes_total[15s]))";
        String writeQuery = "sum by (instance) (rate(node_disk_written_bytes_total[15s]))";

        collectMetricAndSet(dataMap, readQuery, MetricRawData::setDiskReadBps);
        collectMetricAndSet(dataMap, writeQuery, MetricRawData::setDiskWriteBps);

        String readCountQuery = "sum by (instance) (rate(node_disk_reads_completed_total[15s]))";
        String writeCountQuery = "sum by (instance) (rate(node_disk_writes_completed_total[15s]))";

        collectMetricAndSet(dataMap, readCountQuery, (d, v) -> d.setDiskReadCount(v.longValue()));
        collectMetricAndSet(dataMap, writeCountQuery, (d, v) -> d.setDiskWriteCount(v.longValue()));
    }

    private void collectMetricAndSet(
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
                log.error("❌ DiskMetric 저장 실패: equipmentId={} - {}",
                        data.getEquipmentId(), e.getMessage());
            }
        }

        if (failureCount > 0) {
            log.warn("⚠️ DiskMetric 저장 완료: 성공={}, 실패={}", successCount, failureCount);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMetricWithNewTransaction(MetricRawData data) {
        DiskMetric metric = convertToEntity(data);

        DiskMetric existing = diskMetricRepository
                .findByEquipmentIdAndGenerateTime(data.getEquipmentId(), metric.getGenerateTime())
                .orElse(null);

        if (existing != null) {
            updateExisting(existing, metric);
            diskMetricRepository.save(existing);
        } else {
            diskMetricRepository.save(metric);
        }
    }

    private DiskMetric convertToEntity(MetricRawData data) {
        LocalDateTime generateTime = data.getTimestamp() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();

        return DiskMetric.builder()
                .equipmentId(data.getEquipmentId())
                .generateTime(generateTime)
                .totalBytes(data.getTotalDisk())
                .usedBytes(data.getUsedDisk())
                .freeBytes(data.getFreeDisk())
                .ioReadBps(data.getDiskReadBps())
                .ioWriteBps(data.getDiskWriteBps())
                .ioReadCount(data.getDiskReadCount())
                .ioWriteCount(data.getDiskWriteCount())
                .totalInodes(data.getTotalInodes())
                .freeInodes(data.getFreeInodes())
                .build();
    }

    private void updateExisting(DiskMetric existing, DiskMetric newMetric) {
        if (newMetric.getTotalBytes() != null) existing.setTotalBytes(newMetric.getTotalBytes());
        if (newMetric.getUsedBytes() != null) existing.setUsedBytes(newMetric.getUsedBytes());
        if (newMetric.getFreeBytes() != null) existing.setFreeBytes(newMetric.getFreeBytes());
        if (newMetric.getIoReadBps() != null) existing.setIoReadBps(newMetric.getIoReadBps());
        if (newMetric.getIoWriteBps() != null) existing.setIoWriteBps(newMetric.getIoWriteBps());
    }
}