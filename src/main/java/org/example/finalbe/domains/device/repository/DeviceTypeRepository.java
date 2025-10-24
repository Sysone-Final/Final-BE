package org.example.finalbe.domains.device.repository;


import org.example.finalbe.domains.device.domain.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {

    Optional<DeviceType> findByTypeName(String typeName);

    boolean existsByTypeName(String typeName);
}