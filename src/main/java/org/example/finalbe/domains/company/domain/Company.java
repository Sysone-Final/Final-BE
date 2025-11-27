/**
 * 작성자: 황요한
 * 회사(Company) 엔티티
 * - 회사 기본 정보 관리
 * - Soft Delete(논리 삭제) 지원
 * - 부분 수정(updateInfo) 기능 제공
 */
package org.example.finalbe.domains.company.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;
import org.example.finalbe.domains.common.enumdir.DelYN;

@Entity
@Table(
        name = "company",
        indexes = {
                @Index(name = "idx_company_name", columnList = "name"),
                @Index(name = "idx_company_code", columnList = "code")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Long id; // 회사 ID

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // 회사 코드

    @Column(name = "name", nullable = false, length = 200)
    private String name; // 회사명

    @Column(name = "business_number", unique = true, length = 20)
    private String businessNumber; // 사업자등록번호

    @Column(name = "ceo_name", length = 100)
    private String ceoName; // 대표자명

    @Column(name = "phone", length = 20)
    private String phone; // 대표 전화번호

    @Column(name = "fax", length = 20)
    private String fax; // 팩스번호

    @Column(name = "email", length = 100)
    private String email; // 대표 이메일

    @Column(name = "address", length = 500)
    private String address; // 본사 주소

    @Column(name = "website", length = 200)
    private String website; // 웹사이트

    @Column(name = "industry", length = 100)
    private String industry; // 업종

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 회사 설명

    @Column(name = "employee_count")
    private Integer employeeCount; // 직원 수

    @Column(name = "established_date", length = 10)
    private String establishedDate; // 설립일 (YYYY-MM-DD)

    @Column(name = "logo_url", length = 500)
    private String logoUrl; // 로고 이미지 URL

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false, length = 1)
    @Builder.Default
    private DelYN delYn = DelYN.N; // 삭제 여부 (N: 정상, Y: 삭제)

    /**
     * 회사 정보 부분 수정
     * null 또는 빈 값("")은 무시하고 기존 값 유지
     */
    public void updateInfo(
            String name,
            String businessNumber,
            String ceoName,
            String phone,
            String fax,
            String email,
            String address,
            String website,
            String industry,
            String description,
            Integer employeeCount,
            String establishedDate,
            String logoUrl
    ) {
        if (isValid(name)) this.name = name;
        if (isValid(businessNumber)) this.businessNumber = businessNumber;
        if (isValid(ceoName)) this.ceoName = ceoName;
        if (isValid(phone)) this.phone = phone;
        if (isValid(fax)) this.fax = fax;
        if (isValid(email)) this.email = email;
        if (isValid(address)) this.address = address;
        if (isValid(website)) this.website = website;
        if (isValid(industry)) this.industry = industry;
        if (isValid(description)) this.description = description;
        if (employeeCount != null) this.employeeCount = employeeCount;
        if (isValid(establishedDate)) this.establishedDate = establishedDate;
        if (isValid(logoUrl)) this.logoUrl = logoUrl;
    }

    /**
     * Soft Delete (논리 삭제)
     */
    public void softDelete() {
        this.delYn = DelYN.Y;
    }

    /**
     * 삭제 복구
     */
    public void restore() {
        this.delYn = DelYN.N;
    }

    /**
     * 문자열 유효성 체크 (null 또는 공백은 false)
     */
    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
