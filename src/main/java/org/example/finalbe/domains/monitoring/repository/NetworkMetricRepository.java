// NetworkMetricRepository.java
package org.example.finalbe.domains.monitoring.repository;


import org.example.finalbe.domains.monitoring.domain.NetworkMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkMetricRepository extends JpaRepository<NetworkMetric, Long> {
}