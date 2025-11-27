// 작성자: 황요한
// 설명: DeviceType 엔티티에 대한 데이터 접근 레포지토리

package org.example.finalbe.domains.device.repository;

import org.example.finalbe.domains.device.domain.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {
    // DeviceType 엔티티 기본 CRUD 제공
}
