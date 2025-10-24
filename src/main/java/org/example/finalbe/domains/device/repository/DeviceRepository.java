package org.example.finalbe.domains.device.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByDatacenterIdAndDelYn(Long datacenterId, DelYN delYn);

    List<Device> findByRackIdAndDelYn(Long rackId, DelYN delYn);

    boolean existsByDeviceCodeAndDelYn(String deviceCode, DelYN delYn);

    @Query("SELECT d FROM Device d WHERE d.id = :id AND d.delYn = 'N'")
    Optional<Device> findActiveById(@Param("id") Long id);

    @Query("SELECT d FROM Device d " +
            "WHERE d.datacenter.id = :datacenterId " +
            "AND d.delYn = :delYn " +
            "ORDER BY d.gridY, d.gridX")
    List<Device> findByDatacenterIdOrderByPosition(
            @Param("datacenterId") Long datacenterId,
            @Param("delYn") DelYN delYn);
}