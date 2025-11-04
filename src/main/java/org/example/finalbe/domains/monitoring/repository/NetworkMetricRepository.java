// NetworkMetricRepository.java
package org.example.finalbe.domains.monitoring.repository;


import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NetworkMetricRepository extends JpaRepository<NetworkMetric, Long> {
    List<NetworkMetric> findByDeviceIdAndNicNameAndGenerateTimeBetween(
            Integer deviceId, String nicName, LocalDateTime start, LocalDateTime end);
}