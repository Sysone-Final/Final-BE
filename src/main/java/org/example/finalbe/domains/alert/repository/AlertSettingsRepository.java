/**
 * 작성자: 황요한
 * 알림 설정(AlertSettings) 엔티티 Repository
 */
package org.example.finalbe.domains.alert.repository;

import org.example.finalbe.domains.alert.domain.AlertSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertSettingsRepository extends JpaRepository<AlertSettings, Long> {
}
