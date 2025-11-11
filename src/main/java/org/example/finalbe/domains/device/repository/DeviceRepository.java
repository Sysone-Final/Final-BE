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
     * 활성 장치 조회 (ID) - 활성 Rack에 속한 것만
     */
    @Query("SELECT d FROM Device d " +
            "JOIN d.rack r " +
            "WHERE d.id = :id " +
            "AND d.delYn = 'N' " +
            "AND r.delYn = 'N'")
    Optional<Device> findActiveById(@Param("id") Long id);

    /**
     * 서버실별 장치 조회 (위치순 정렬) - 활성 Rack에 속한 것만
     */
    @Query("SELECT d FROM Device d " +
            "JOIN d.rack r " +
            "WHERE r.serverRoom.id = :serverRoomId " +
            "AND d.delYn = :delYn " +
            "AND r.delYn = 'N' " +
            "ORDER BY d.gridY, d.gridX")
    List<Device> findByServerRoomIdOrderByPosition(
            @Param("serverRoomId") Long serverRoomId,
            @Param("delYn") DelYN delYn);

    /**
     * 특정 Rack의 활성 장치 조회 (랙 삭제 시 사용)
     */
    @Query("SELECT d FROM Device d " +
            "WHERE d.rack.id = :rackId " +
            "AND d.delYn = 'N'")
    List<Device> findActiveByRackId(@Param("rackId") Long rackId);

    /**
     * 모든 활성 장치 조회 (활성 Rack에 속한 것만)
     */
    @Query("SELECT d FROM Device d " +
            "JOIN d.rack r " +
            "WHERE d.delYn = 'N' " +
            "AND r.delYn = 'N'")
    List<Device> findAllActive();
}