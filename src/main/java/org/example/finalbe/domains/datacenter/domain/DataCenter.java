package org.example.finalbe.domains.datacenter.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.member.domain.Member;

import java.math.BigDecimal;

/**
 * 전산실(데이터센터) 엔티티
 *
 * - JPA(Hibernate): 객체-관계 매핑을 통해 datacenter 테이블과 매핑
 * - Lombok: @Getter, @Builder 등으로 보일러플레이트 코드 제거
 * - BigDecimal: 정밀한 숫자 계산이 필요한 전력, 냉각, 면적 등에 사용
 * - Index: 조회 성능 향상을 위해 name, code, status 컬럼에 인덱스 생성
 * - Soft Delete: delYn으로 논리 삭제 처리 (BaseTimeEntity 상속)
 */
@Entity // JPA 엔티티임을 선언. 이 클래스는 데이터베이스 datacenter 테이블과 매핑됨
@Table(name = "datacenter", indexes = { // 실제 데이터베이스 테이블 이름 지정
        @Index(name = "idx_datacenter_name", columnList = "name"), // 전산실 이름 검색 최적화
        @Index(name = "idx_datacenter_code", columnList = "code"), // 전산실 코드 조회 최적화
        @Index(name = "idx_datacenter_status", columnList = "status") // 상태별 필터링 최적화
})
@NoArgsConstructor // JPA는 기본 생성자가 필요함 (Hibernate가 리플렉션으로 객체 생성 시 사용)
@AllArgsConstructor // 모든 필드를 받는 생성자 (Builder 패턴과 함께 사용)
@Getter // 모든 필드의 getter 메서드 자동 생성
@Builder // 빌더 패턴을 사용하여 객체 생성 (가독성 향상)
public class DataCenter extends BaseTimeEntity { // BaseTimeEntity를 상속받아 createdAt, updatedAt, delYn 자동 관리

    @Id // Primary Key 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment 전략 (PostgreSQL의 SERIAL 타입 사용)
    @Column(name = "datacenter_id") // 데이터베이스 컬럼 이름 명시
    private Long id; // 전산실 고유 식별자

    @Column(name = "name", nullable = false, length = 100) // NOT NULL 제약조건, 최대 100자
    private String name; // 전산실 이름 (예: "서울 IDC", "부산 데이터센터")

    @Column(name = "code", unique = true, length = 50) // UNIQUE 제약조건, 중복 불가
    private String code; // 전산실 코드 (예: "DC-SEL-001", "IDC-BSN-002")

    @Column(name = "location", length = 255) // 주소는 길 수 있으므로 255자 제한
    private String location; // 전산실 위치/주소 (예: "서울시 강남구 테헤란로 123")

    @Column(name = "floor", length = 50) // 층수/건물 정보
    private String floor; // 전산실이 위치한 층 (예: "지하 2층", "5층", "B동 3층")

    @Column(name = "rows") // 랙 배치 행 수
    private Integer rows; // 전산실 내 랙이 배치된 행(가로) 개수

    @Column(name = "columns") // 랙 배치 열 수
    private Integer columns; // 전산실 내 랙이 배치된 열(세로) 개수

    @Column(name = "background_image_url", length = 500) // 이미지 URL은 길 수 있으므로 500자
    private String backgroundImageUrl; // 전산실 평면도 이미지 URL (UI에서 배경으로 사용)

    @Enumerated(EnumType.STRING) // Enum을 문자열로 저장 (ORDINAL은 순서가 바뀌면 문제 발생)
    @Column(name = "status", length = 20) // 상태는 영문자로 저장 (ACTIVE, INACTIVE, MAINTENANCE)
    private DataCenterStatus status; // 전산실 운영 상태

    @Lob // Large Object: 긴 텍스트를 저장 (PostgreSQL의 TEXT 타입)
    @Column(name = "description", columnDefinition = "TEXT") // 명시적으로 TEXT 타입 지정
    private String description; // 전산실 설명 (용도, 특이사항 등)

    // === 시설 용량 관련 필드 (BigDecimal 사용) ===
    // BigDecimal: 금융, 통계 등 정밀한 계산이 필요한 경우 사용 (float, double는 부동소수점 오차 발생)

    @Column(name = "total_area", precision = 10, scale = 2) // 총 10자리, 소수점 2자리
    private BigDecimal totalArea; // 전산실 총 면적 (단위: m²)

    @Column(name = "total_power_capacity", precision = 10, scale = 2) // 총 10자리, 소수점 2자리
    private BigDecimal totalPowerCapacity; // 총 전력 용량 (단위: kW)

    @Column(name = "total_cooling_capacity", precision = 10, scale = 2) // 총 10자리, 소수점 2자리
    private BigDecimal totalCoolingCapacity; // 총 냉각 용량 (단위: kW)

    // === 랙 관리 관련 필드 ===

    @Column(name = "max_rack_count") // 최대 설치 가능한 랙 개수
    private Integer maxRackCount; // 전산실 설계 상 최대 랙 수용 개수

    @Column(name = "current_rack_count") // 현재 설치된 랙 개수
    @Builder.Default // Builder 사용 시 기본값 0으로 설정
    private Integer currentRackCount = 0; // 실시간 랙 개수 (랙 추가/제거 시 증감)
    // 주의: 정합성 문제 방지를 위해 조회 시점에 계산하거나 이벤트로 관리하는 것을 권장

    // === 환경 관리 기준값 ===

    @Column(name = "temperature_min", precision = 5, scale = 2) // 온도는 소수점 2자리까지
    private BigDecimal temperatureMin; // 최저 허용 온도 (단위: ℃)

    @Column(name = "temperature_max", precision = 5, scale = 2) // 온도는 소수점 2자리까지
    private BigDecimal temperatureMax; // 최고 허용 온도 (단위: ℃)

    @Column(name = "humidity_min", precision = 5, scale = 2) // 습도는 소수점 2자리까지
    private BigDecimal humidityMin; // 최저 허용 습도 (단위: %)

    @Column(name = "humidity_max", precision = 5, scale = 2) // 습도는 소수점 2자리까지
    private BigDecimal humidityMax; // 최고 허용 습도 (단위: %)

    // === 관계 매핑 ===

    @ManyToOne(fetch = FetchType.LAZY) // 다대일 관계, 지연 로딩 (필요할 때만 조회)
    @JoinColumn(name = "manager_id", nullable = false) // 외래키 컬럼 지정, NOT NULL
    private Member manager; // 전산실 담당자 (필수)
    // FetchType.LAZY: manager 정보가 실제로 필요할 때만 DB에서 조회 (성능 최적화)

    // === 비즈니스 로직 메서드 ===

    /**
     * 전산실 정보 업데이트
     * null이 아닌 값만 업데이트하여 부분 수정 가능
     */
    public void updateInfo(
            String name, // 전산실 이름
            String code, // 전산실 코드
            String location, // 위치/주소
            String floor, // 층수
            Integer rows, // 행 수
            Integer columns, // 열 수
            String backgroundImageUrl, // 배경 이미지 URL
            DataCenterStatus status, // 운영 상태
            String description, // 설명
            BigDecimal totalArea, // 총 면적
            BigDecimal totalPowerCapacity, // 총 전력 용량
            BigDecimal totalCoolingCapacity, // 총 냉각 용량
            Integer maxRackCount, // 최대 랙 개수
            BigDecimal temperatureMin, // 최저 온도
            BigDecimal temperatureMax, // 최고 온도
            BigDecimal humidityMin, // 최저 습도
            BigDecimal humidityMax, // 최고 습도
            Member manager // 담당자
    ) {
        // null이 아닌 값만 업데이트 (부분 수정 가능)
        if (name != null && !name.trim().isEmpty()) this.name = name; // 이름 변경
        if (code != null && !code.trim().isEmpty()) this.code = code; // 코드 변경
        if (location != null) this.location = location; // 위치 변경
        if (floor != null) this.floor = floor; // 층 변경
        if (rows != null) this.rows = rows; // 행 수 변경
        if (columns != null) this.columns = columns; // 열 수 변경
        if (backgroundImageUrl != null) this.backgroundImageUrl = backgroundImageUrl; // 이미지 URL 변경
        if (status != null) this.status = status; // 상태 변경
        if (description != null) this.description = description; // 설명 변경
        if (totalArea != null) this.totalArea = totalArea; // 면적 변경
        if (totalPowerCapacity != null) this.totalPowerCapacity = totalPowerCapacity; // 전력 용량 변경
        if (totalCoolingCapacity != null) this.totalCoolingCapacity = totalCoolingCapacity; // 냉각 용량 변경
        if (maxRackCount != null) this.maxRackCount = maxRackCount; // 최대 랙 개수 변경
        if (temperatureMin != null) this.temperatureMin = temperatureMin; // 최저 온도 변경
        if (temperatureMax != null) this.temperatureMax = temperatureMax; // 최고 온도 변경
        if (humidityMin != null) this.humidityMin = humidityMin; // 최저 습도 변경
        if (humidityMax != null) this.humidityMax = humidityMax; // 최고 습도 변경
        if (manager != null) this.manager = manager; // 담당자 변경

        this.updateTimestamp(); // BaseTimeEntity의 updatedAt 갱신
    }

    /**
     * 랙 추가 시 현재 랙 개수 증가
     * 최대 랙 개수 초과 방지
     */
    public void incrementRackCount() {
        // currentRackCount가 null이면 0으로 초기화
        if (this.currentRackCount == null) {
            this.currentRackCount = 0;
        }
        // 최대 랙 개수를 초과하면 예외 발생
        if (this.currentRackCount >= this.maxRackCount) {
            throw new IllegalStateException("최대 랙 수를 초과할 수 없습니다.");
        }
        this.currentRackCount++; // 랙 개수 1 증가
    }

    /**
     * 랙 제거 시 현재 랙 개수 감소
     * 0 미만으로 감소 방지
     */
    public void decrementRackCount() {
        // currentRackCount가 null이거나 0 이하면 예외 발생
        if (this.currentRackCount == null || this.currentRackCount <= 0) {
            throw new IllegalStateException("현재 랙 수가 0입니다.");
        }
        this.currentRackCount--; // 랙 개수 1 감소
    }

    /**
     * 사용 가능한 랙 개수 계산
     * 최대 랙 개수 - 현재 랙 개수
     */
    public int getAvailableRackCount() {
        // maxRackCount와 currentRackCount가 null이면 0으로 처리
        return (maxRackCount != null ? maxRackCount : 0) - (currentRackCount != null ? currentRackCount : 0);
    }
}