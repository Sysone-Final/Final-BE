// SystemMetricRepository.java
package org.example.finalbe.domains.monitoring.repository;


import org.example.finalbe.domains.monitoring.domain.SystemMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {

}