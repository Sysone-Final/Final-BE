package org.example.finalbe.domains.common.enumdir;

/**
 * 메트릭 집계 레벨
 * 데이터 조회 시 집계 단위 지정
 */
public enum AggregationLevel {

    /**
     * 원본 데이터 (5초 단위)
     * - 최근 1시간 이내 조회 시 권장
     * - 실시간 모니터링용
     */
    RAW,

    /**
     * 1분 단위 집계
     * - 1시간 ~ 6시간 조회 시 권장
     * - 단기 추세 분석용
     */
    MIN,

    /**
     * 5분 단위 집계
     * - 6시간 ~ 24시간 조회 시 권장
     * - 일간 추세 분석용
     */
    MIN5,

    /**
     * 1시간 단위 집계
     * - 24시간 ~ 30일 조회 시 권장
     * - 장기 추세 분석용
     */
    HOUR
}