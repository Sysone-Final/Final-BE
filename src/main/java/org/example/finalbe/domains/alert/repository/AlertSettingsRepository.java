package org.example.finalbe.domains.alert.repository;

import org.example.finalbe.domains.alert.domain.AlertSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AlertSettings 데이터 접근 계층
 * 전역 알림 설정 관리
 */
@Repository
public interface AlertSettingsRepository extends JpaRepository<AlertSettings, Long> {

    /**
     * 전역 알림 설정 조회 (단일 레코드)
     * 실무에서는 ID=1L 고정으로 사용
     */
    @Query("SELECT a FROM AlertSettings a WHERE a.id = 1L")
    Optional<AlertSettings> findGlobalSettings();

    /**
     * 전역 설정 존재 여부 확인
     */
    @Query("SELECT COUNT(a) > 0 FROM AlertSettings a WHERE a.id = 1L")
    boolean existsGlobalSettings();
}