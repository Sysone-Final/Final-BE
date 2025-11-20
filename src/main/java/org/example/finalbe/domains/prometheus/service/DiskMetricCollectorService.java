package org.example.finalbe.domains.prometheus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.example.finalbe.domains.monitoring.repository.DiskMetricRepository;
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
public class DiskMetricCollectorService {

    private final PrometheusQueryService prometheusQuery;
    private final DiskMetricRepository diskMetricRepository;

    public void collectAndPopulate(Map<Long, MetricRawData> dataMap) {
        collectDiskSpace(dataMap);
        collectDiskInodes(dataMap);
        collectDiskIO(dataMap);
    }

    private void collectDiskSpace(Map<Long, MetricRawData> dataMap) {
        String totalQuery = "sum by (instance) (node_filesystem_size_bytes{fstype!~\"tmpfs|devtmpfs|overlay|shm\"})";
        List<PrometheusResponse.PrometheusResult> totalResults = prometheusQuery.query(totalQuery);

        for (PrometheusResponse.PrometheusResult result : totalResults) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setDiskTotalBytes(value.longValue());
                }
            }
        }

        String freeQuery = "sum by (instance) (node_filesystem_free_bytes{fstype!~\"tmpfs|devtmpfs|overlay|shm\"})";
        List<PrometheusResponse.PrometheusResult> freeResults = prometheusQuery.query(freeQuery);

        for (PrometheusResponse.PrometheusResult result : freeResults) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setDiskFreeBytes(value.longValue());
                    if (data.getDiskTotalBytes() != null) {
                        data.setDiskUsedBytes(data.getDiskTotalBytes() - value.longValue());
                    }
                }
            }
        }
    }

    private void collectDiskInodes(Map<Long, MetricRawData> dataMap) {
        String totalQuery = "sum by (instance) (node_filesystem_files{fstype!~\"tmpfs|devtmpfs|overlay|shm\"})";
        List<PrometheusResponse.PrometheusResult> totalResults = prometheusQuery.query(totalQuery);

        for (PrometheusResponse.PrometheusResult result : totalResults) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setDiskTotalInodes(value.longValue());
                }
            }
        }

        String freeQuery = "sum by (instance) (node_filesystem_files_free{fstype!~\"tmpfs|devtmpfs|overlay|shm\"})";
        List<PrometheusResponse.PrometheusResult> freeResults = prometheusQuery.query(freeQuery);

        for (PrometheusResponse.PrometheusResult result : freeResults) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    data.setDiskFreeInodes(value.longValue());
                }
            }
        }
    }

    private void collectDiskIO(Map<Long, MetricRawData> dataMap) {
        String readQuery = "sum by (instance) (rate(node_disk_read_bytes_total[5s]))";
        collectDiskIOMetric(dataMap, readQuery, MetricRawData::setDiskReadBps);

        String writeQuery = "sum by (instance) (rate(node_disk_written_bytes_total[5s]))";
        collectDiskIOMetric(dataMap, writeQuery, MetricRawData::setDiskWriteBps);

        String readCountQuery = "sum by (instance) (rate(node_disk_reads_completed_total[5s]))";
        collectDiskIOCountMetric(dataMap, readCountQuery, MetricRawData::setDiskReadCount);

        String writeCountQuery = "sum by (instance) (rate(node_disk_writes_completed_total[5s]))";
        collectDiskIOCountMetric(dataMap, writeCountQuery, MetricRawData::setDiskWriteCount);

        String ioTimeQuery = "avg by (instance) (rate(node_disk_io_time_seconds_total[5s]) * 100)";
        collectDiskIOMetric(dataMap, ioTimeQuery, MetricRawData::setDiskIoTimePercentage);
    }

    private void collectDiskIOMetric(Map<Long, MetricRawData> dataMap, String query,
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

    private void collectDiskIOCountMetric(Map<Long, MetricRawData> dataMap, String query,
                                          java.util.function.BiConsumer<MetricRawData, Long> setter) {
        List<PrometheusResponse.PrometheusResult> results = prometheusQuery.query(query);

        for (PrometheusResponse.PrometheusResult result : results) {
            String instance = result.getInstance();
            Double value = result.getValue();

            if (instance != null && value != null) {
                MetricRawData data = findDataByInstance(dataMap, instance);
                if (data != null) {
                    setter.accept(data, (long) (value * 5));
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

                log.debug("  ✓ DiskMetric 저장: equipmentId={}", data.getEquipmentId());

            } catch (Exception e) {
                log.error("❌ DiskMetric 저장 실패: equipmentId={} - {}",
                        data.getEquipmentId(), e.getMessage());
            }
        }
    }

    private DiskMetric convertToEntity(MetricRawData data) {
        LocalDateTime generateTime = data.getTimestamp() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(data.getTimestamp()), ZoneId.systemDefault())
                : LocalDateTime.now();

        Double usedPercentage = null;
        if (data.getDiskTotalBytes() != null && data.getDiskTotalBytes() > 0
                && data.getDiskUsedBytes() != null) {
            usedPercentage = (data.getDiskUsedBytes() * 100.0) / data.getDiskTotalBytes();
        }

        Double usedInodePercentage = null;
        Long usedInodes = null;
        if (data.getDiskTotalInodes() != null && data.getDiskFreeInodes() != null) {
            usedInodes = data.getDiskTotalInodes() - data.getDiskFreeInodes();
            if (data.getDiskTotalInodes() > 0) {
                usedInodePercentage = (usedInodes * 100.0) / data.getDiskTotalInodes();
            }
        }

        return DiskMetric.builder()
                .equipmentId(data.getEquipmentId())
                .generateTime(generateTime)
                .totalBytes(data.getDiskTotalBytes())
                .usedBytes(data.getDiskUsedBytes())
                .freeBytes(data.getDiskFreeBytes())
                .usedPercentage(usedPercentage)
                .ioReadBps(data.getDiskReadBps())
                .ioWriteBps(data.getDiskWriteBps())
                .ioTimePercentage(data.getDiskIoTimePercentage())
                .ioReadCount(data.getDiskReadCount())
                .ioWriteCount(data.getDiskWriteCount())
                .totalInodes(data.getDiskTotalInodes())
                .usedInodes(usedInodes)
                .freeInodes(data.getDiskFreeInodes())
                .usedInodePercentage(usedInodePercentage)
                .build();
    }

    private void updateExisting(DiskMetric existing, DiskMetric newMetric) {
        existing.setTotalBytes(newMetric.getTotalBytes());
        existing.setUsedBytes(newMetric.getUsedBytes());
        existing.setFreeBytes(newMetric.getFreeBytes());
        existing.setUsedPercentage(newMetric.getUsedPercentage());
        existing.setIoReadBps(newMetric.getIoReadBps());
        existing.setIoWriteBps(newMetric.getIoWriteBps());
        existing.setIoTimePercentage(newMetric.getIoTimePercentage());
        existing.setIoReadCount(newMetric.getIoReadCount());
        existing.setIoWriteCount(newMetric.getIoWriteCount());
        existing.setTotalInodes(newMetric.getTotalInodes());
        existing.setUsedInodes(newMetric.getUsedInodes());
        existing.setFreeInodes(newMetric.getFreeInodes());
        existing.setUsedInodePercentage(newMetric.getUsedInodePercentage());
    }
}