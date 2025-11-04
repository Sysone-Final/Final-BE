// EnvironmentMetricRepository.java
package org.example.finalbe.domains.monitoring.repository;

import org.example.finalbe.domains.monitoring.domain.EnvironmentMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EnvironmentMetricRepository extends JpaRepository<EnvironmentMetric, Long> {

}