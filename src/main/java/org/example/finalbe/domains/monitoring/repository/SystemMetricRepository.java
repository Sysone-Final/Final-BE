// SystemMetricRepository.java
package org.example.finalbe.domains.monitoring.repository;


import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {
    SystemMetric findTopByDeviceIdOrderByGenerateTimeDesc(Integer deviceId);
    List<SystemMetric> findByDeviceIdAndGenerateTimeBetween(
            Integer deviceId, LocalDateTime start, LocalDateTime end);
}