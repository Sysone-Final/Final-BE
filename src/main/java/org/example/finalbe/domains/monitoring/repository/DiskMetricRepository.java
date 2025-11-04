// DiskMetricRepository.java
package org.example.finalbe.domains.monitoring.repository;


import org.example.finalbe.domains.monitoring.domain.DiskMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiskMetricRepository extends JpaRepository<DiskMetric, Long> {
}