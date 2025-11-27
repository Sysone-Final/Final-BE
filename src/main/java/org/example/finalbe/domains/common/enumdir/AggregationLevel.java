/**
 * 작성자: 황요한
 * 메트릭 집계 레벨 열거형
 */
package org.example.finalbe.domains.common.enumdir;

public enum AggregationLevel {

    /**
     * 원본 데이터 (5초 단위)
     */
    RAW,

    /**
     * 1분 단위 집계
     */
    MIN,

    /**
     * 5분 단위 집계
     */
    MIN5,

    /**
     * 1시간 단위 집계
     */
    HOUR,

    /**
     * 1일 단위 집계
     */
    DAY
}