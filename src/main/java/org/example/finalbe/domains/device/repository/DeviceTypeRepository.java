package org.example.finalbe.domains.device.repository;

import org.example.finalbe.domains.device.domain.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DeviceType 데이터 접근 계층
 */
@Repository
public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {

}