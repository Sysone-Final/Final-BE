package org.example.finalbe.domains.device.repository;

import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Device 데이터 접근 계층
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * 장치 코드 중복 체크
     */
    boolean existsByDeviceCodeAndDelYn(String deviceCode, DelYN delYn);

    /**
     * 활성 장치 조회 (ID)
     */
    @Query("SELECT d FROM Device d WHERE d.id = :id AND d.delYn = 'N'")
    Optional<Device> findActiveById(@Param("id") Long id);

    /**
     * 전산실별 장치 조회 (위치순 정렬)
     */
    @Query("SELECT d FROM Device d " +
            "WHERE d.datacenter.id = :datacenterId " +
            "AND d.delYn = :delYn " +
            "ORDER BY d.gridY, d.gridX")
    List<Device> findByDatacenterIdOrderByPosition(
            @Param("datacenterId") Long datacenterId,
            @Param("delYn") DelYN delYn);
}