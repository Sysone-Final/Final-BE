package org.example.finalbe.domains.company.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.finalbe.domains.common.domain.BaseTimeEntity;

@Entity
@Table(name = "company", indexes = {
        @Index(name = "idx_company_name", columnList = "name"),
        @Index(name = "idx_company_code", columnList = "code")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Long id;

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
    private String address; // 주소

    @Column(name = "website", length = 200)
    private String website; // 웹사이트

    @Column(name = "industry", length = 100)
    private String industry; // 업종

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 설명

    @Column(name = "employee_count")
    private Integer employeeCount; // 직원 수

    @Column(name = "established_date", length = 10)
    private String establishedDate; // 설립일 (YYYY-MM-DD)

    @Column(name = "logo_url", length = 500)
    private String logoUrl; // 로고 URL

    // 정보 업데이트 메서드
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
        if (name != null) this.name = name;
        if (businessNumber != null) this.businessNumber = businessNumber;
        if (ceoName != null) this.ceoName = ceoName;
        if (phone != null) this.phone = phone;
        if (fax != null) this.fax = fax;
        if (email != null) this.email = email;
        if (address != null) this.address = address;
        if (website != null) this.website = website;
        if (industry != null) this.industry = industry;
        if (description != null) this.description = description;
        if (employeeCount != null) this.employeeCount = employeeCount;
        if (establishedDate != null) this.establishedDate = establishedDate;
        if (logoUrl != null) this.logoUrl = logoUrl;
        this.updateTimestamp();
    }
}
