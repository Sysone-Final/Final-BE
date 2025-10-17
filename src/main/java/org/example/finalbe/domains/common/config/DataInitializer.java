package org.example.finalbe.domains.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.common.enumdir.DataCenterStatus;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.enumdir.UserStatus;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.member.domain.Address;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final DataCenterRepository dataCenterRepository;
    private final CompanyDataCenterRepository companyDataCenterRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=".repeat(80));
        log.info("초기 더미 데이터 로딩 시작...");
        log.info("=".repeat(80));

        try {
            // 1. 회사 데이터 생성
            List<Company> companies = createCompanies();
            log.info("✅ {} 개의 회사 생성 완료", companies.size());

            // 2. 사용자 데이터 생성
            List<Member> members = createMembers(companies);
            log.info("✅ {} 명의 사용자 생성 완료", members.size());

            // 3. 전산실 데이터 생성
            List<DataCenter> dataCenters = createDataCenters(members);
            log.info("✅ {} 개의 전산실 생성 완료", dataCenters.size());

            // 4. 회사-전산실 매핑 생성
            List<CompanyDataCenter> mappings = createCompanyDataCenterMappings(companies, dataCenters, members);
            log.info("✅ {} 개의 회사-전산실 매핑 생성 완료", mappings.size());

            log.info("=".repeat(80));
            log.info("🎉 초기 더미 데이터 로딩 완료!");
            printTestAccounts(members);
            log.info("=".repeat(80));

        } catch (Exception e) {
            log.error("❌ 초기 데이터 로딩 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private List<Company> createCompanies() {
        List<Company> companies = new ArrayList<>();

        companies.add(Company.builder()
                .code("COMP001")
                .name("테스트 회사")
                .businessNumber("123-45-67890")
                .ceoName("홍길동")
                .phone("02-1234-5678")
                .fax("02-1234-5679")
                .email("info@testcompany.com")
                .address("서울시 강남구 테헤란로 123")
                .website("https://www.testcompany.com")
                .industry("IT")
                .description("IT 서비스 전문 기업")
                .employeeCount(100)
                .establishedDate("2020-01-01")
                .logoUrl("https://example.com/logo.png")
                .build());

        companies.add(Company.builder()
                .code("COMP002")
                .name("데이터센터 운영사")
                .businessNumber("234-56-78901")
                .ceoName("김철수")
                .phone("02-2345-6789")
                .fax("02-2345-6780")
                .email("contact@dcoperator.com")
                .address("서울시 서초구 반포대로 456")
                .website("https://www.dcoperator.com")
                .industry("데이터센터")
                .description("데이터센터 운영 전문 기업")
                .employeeCount(50)
                .establishedDate("2018-06-15")
                .logoUrl("https://example.com/logo2.png")
                .build());

        companies.add(Company.builder()
                .code("COMP003")
                .name("클라우드 솔루션")
                .businessNumber("345-67-89012")
                .ceoName("이영희")
                .phone("02-3456-7890")
                .fax("02-3456-7891")
                .email("info@cloudsolution.com")
                .address("서울시 송파구 올림픽로 789")
                .website("https://www.cloudsolution.com")
                .industry("클라우드")
                .description("클라우드 서비스 제공 업체")
                .employeeCount(200)
                .establishedDate("2015-03-20")
                .logoUrl("https://example.com/logo3.png")
                .build());

        return companyRepository.saveAll(companies);
    }

    private List<Member> createMembers(List<Company> companies) {
        List<Member> members = new ArrayList<>();
        String password = passwordEncoder.encode("password123");

        // 각 회사마다 ADMIN, OPERATOR, VIEWER 생성
        int userIndex = 1;
        for (Company company : companies) {
            // ADMIN
            members.add(Member.builder()
                    .username("admin" + userIndex)
                    .password(password)
                    .name("관리자" + userIndex)
                    .email("admin" + userIndex + "@" + company.getCode().toLowerCase() + ".com")
                    .phone("010-1000-" + String.format("%04d", userIndex))
                    .status(UserStatus.ACTIVE)
                    .role(Role.ADMIN)
                    .company(company)
                    .address(Address.builder()
                            .city("서울시")
                            .street("강남구 테헤란로 " + (100 + userIndex))
                            .zipcode("06000")
                            .build())
                    .build());

            // OPERATOR
            members.add(Member.builder()
                    .username("operator" + userIndex)
                    .password(password)
                    .name("운영자" + userIndex)
                    .email("operator" + userIndex + "@" + company.getCode().toLowerCase() + ".com")
                    .phone("010-2000-" + String.format("%04d", userIndex))
                    .status(UserStatus.ACTIVE)
                    .role(Role.OPERATOR)
                    .company(company)
                    .address(Address.builder()
                            .city("서울시")
                            .street("서초구 반포대로 " + (200 + userIndex))
                            .zipcode("06500")
                            .build())
                    .build());

            // VIEWER
            members.add(Member.builder()
                    .username("viewer" + userIndex)
                    .password(password)
                    .name("조회자" + userIndex)
                    .email("viewer" + userIndex + "@" + company.getCode().toLowerCase() + ".com")
                    .phone("010-3000-" + String.format("%04d", userIndex))
                    .status(UserStatus.ACTIVE)
                    .role(Role.VIEWER)
                    .company(company)
                    .address(Address.builder()
                            .city("서울시")
                            .street("송파구 올림픽로 " + (300 + userIndex))
                            .zipcode("05500")
                            .build())
                    .build());

            userIndex++;
        }

        return memberRepository.saveAll(members);
    }

    private List<DataCenter> createDataCenters(List<Member> members) {
        List<DataCenter> dataCenters = new ArrayList<>();

        // 첫 번째 회사의 ADMIN을 매니저로 사용
        Member manager1 = members.get(0); // COMP001의 admin1
        Member manager2 = members.get(3); // COMP002의 admin2

        dataCenters.add(DataCenter.builder()
                .name("서울 제1전산실")
                .code("DC001")
                .location("서울시 구로구 디지털로 300")
                .floor("3F")
                .rows(10)
                .columns(20)
                .backgroundImageUrl("https://example.com/datacenter1.png")
                .status(DataCenterStatus.ACTIVE)
                .description("서울 메인 데이터센터")
                .totalArea(new BigDecimal("1000.50"))
                .totalPowerCapacity(new BigDecimal("2000.00"))
                .totalCoolingCapacity(new BigDecimal("1500.00"))
                .maxRackCount(200)
                .currentRackCount(50)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager1)
                .createdBy(manager1.getUsername())
                .build());

        dataCenters.add(DataCenter.builder()
                .name("서울 제2전산실")
                .code("DC002")
                .location("서울시 금천구 가산디지털로 200")
                .floor("5F")
                .rows(8)
                .columns(15)
                .backgroundImageUrl("https://example.com/datacenter2.png")
                .status(DataCenterStatus.ACTIVE)
                .description("서울 백업 데이터센터")
                .totalArea(new BigDecimal("800.00"))
                .totalPowerCapacity(new BigDecimal("1500.00"))
                .totalCoolingCapacity(new BigDecimal("1200.00"))
                .maxRackCount(120)
                .currentRackCount(30)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager1)
                .createdBy(manager1.getUsername())
                .build());

        dataCenters.add(DataCenter.builder()
                .name("부산 전산실")
                .code("DC003")
                .location("부산시 해운대구 센텀로 100")
                .floor("2F")
                .rows(6)
                .columns(12)
                .backgroundImageUrl("https://example.com/datacenter3.png")
                .status(DataCenterStatus.ACTIVE)
                .description("부산 지역 데이터센터")
                .totalArea(new BigDecimal("600.00"))
                .totalPowerCapacity(new BigDecimal("1000.00"))
                .totalCoolingCapacity(new BigDecimal("800.00"))
                .maxRackCount(72)
                .currentRackCount(20)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager2)
                .createdBy(manager2.getUsername())
                .build());

        dataCenters.add(DataCenter.builder()
                .name("대전 전산실")
                .code("DC004")
                .location("대전시 유성구 테크노로 50")
                .floor("1F")
                .rows(5)
                .columns(10)
                .backgroundImageUrl("https://example.com/datacenter4.png")
                .status(DataCenterStatus.MAINTENANCE)
                .description("대전 연구단지 데이터센터 (점검중)")
                .totalArea(new BigDecimal("500.00"))
                .totalPowerCapacity(new BigDecimal("800.00"))
                .totalCoolingCapacity(new BigDecimal("600.00"))
                .maxRackCount(50)
                .currentRackCount(10)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager2)
                .createdBy(manager2.getUsername())
                .build());

        return dataCenterRepository.saveAll(dataCenters);
    }

    private List<CompanyDataCenter> createCompanyDataCenterMappings(
            List<Company> companies,
            List<DataCenter> dataCenters,
            List<Member> members) {

        List<CompanyDataCenter> mappings = new ArrayList<>();

        // COMP001 (테스트 회사) -> DC001, DC002 접근 가능
        mappings.add(CompanyDataCenter.builder()
                .company(companies.get(0))
                .dataCenter(dataCenters.get(0))
                .description("메인 전산실 사용 계약")
                .grantedBy(members.get(0).getUsername())
                .build());

        mappings.add(CompanyDataCenter.builder()
                .company(companies.get(0))
                .dataCenter(dataCenters.get(1))
                .description("백업 전산실 사용 계약")
                .grantedBy(members.get(0).getUsername())
                .build());

        // COMP002 (데이터센터 운영사) -> 모든 DC 접근 가능
        for (DataCenter dc : dataCenters) {
            mappings.add(CompanyDataCenter.builder()
                    .company(companies.get(1))
                    .dataCenter(dc)
                    .description("전산실 운영사 - 전체 접근 권한")
                    .grantedBy(members.get(3).getUsername())
                    .build());
        }

        // COMP003 (클라우드 솔루션) -> DC001, DC003 접근 가능
        mappings.add(CompanyDataCenter.builder()
                .company(companies.get(2))
                .dataCenter(dataCenters.get(0))
                .description("서울 메인 전산실 사용")
                .grantedBy(members.get(0).getUsername())
                .build());

        mappings.add(CompanyDataCenter.builder()
                .company(companies.get(2))
                .dataCenter(dataCenters.get(2))
                .description("부산 전산실 사용")
                .grantedBy(members.get(3).getUsername())
                .build());

        return companyDataCenterRepository.saveAll(mappings);
    }

    private void printTestAccounts(List<Member> members) {
        log.info("");
        log.info("💡 테스트 계정 (비밀번호: password123)");
        log.info("-".repeat(80));

        for (Member member : members) {
            log.info("   {} / {} / {}",
                    member.getUsername(),
                    member.getRole(),
                    member.getCompany().getName());
        }

        log.info("");
    }
}