package org.example.finalbe.domains.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.company.domain.Company;
import org.example.finalbe.domains.company.repository.CompanyRepository;
import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.common.enumdir.*;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.department.domain.Department;
import org.example.finalbe.domains.department.domain.MemberDepartment;
import org.example.finalbe.domains.department.repository.DepartmentRepository;
import org.example.finalbe.domains.department.repository.MemberDepartmentRepository;
import org.example.finalbe.domains.device.domain.Device;
import org.example.finalbe.domains.device.domain.DeviceType;
import org.example.finalbe.domains.device.repository.DeviceRepository;
import org.example.finalbe.domains.device.repository.DeviceTypeRepository;
import org.example.finalbe.domains.equipment.domain.Equipment;
import org.example.finalbe.domains.equipment.repository.EquipmentRepository;
import org.example.finalbe.domains.member.domain.Address;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final RackRepository rackRepository;
    private final EquipmentRepository equipmentRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final MemberDepartmentRepository memberDepartmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=".repeat(80));
        log.info("초기 더미 데이터 로딩 시작...");
        log.info("=".repeat(80));

        try {
            // ⭐ 기존 데이터가 있으면 초기화 건너뛰기
            if (companyRepository.count() > 0) {
                log.info("✅ 기존 데이터가 존재하여 초기화를 건너뜁니다.");
                log.info("   - 회사: {}개", companyRepository.count());
                log.info("   - 사용자: {}명", memberRepository.count());
                log.info("   - 전산실: {}개", dataCenterRepository.count());
                log.info("   - 랙: {}개", rackRepository.count());
                log.info("   - 장비: {}개", equipmentRepository.count());
                log.info("   - 장치: {}개", deviceRepository.count());
                log.info("=".repeat(80));
                return;
            }

            // 1. 회사 데이터 생성
            List<Company> companies = createCompanies();
            log.info("✅ {} 개의 회사 생성 완료", companies.size());

            // 2. 사용자 데이터 생성
            List<Member> members = createMembers(companies);
            log.info("✅ {} 명의 사용자 생성 완료", members.size());

            // 3. 부서 데이터 생성
            List<Department> departments = createDepartments(companies, members);
            log.info("✅ {} 개의 부서 생성 완료", departments.size());

            // 4. 사용자-부서 매핑 생성
            List<MemberDepartment> memberDepartmentMappings = createMemberDepartmentMappings(members, departments);
            log.info("✅ {} 개의 사용자-부서 매핑 생성 완료", memberDepartmentMappings.size());

            // 5. 전산실 데이터 생성
            List<DataCenter> dataCenters = createDataCenters(members);
            log.info("✅ {} 개의 전산실 생성 완료", dataCenters.size());

            // 6. 회사-전산실 매핑 생성
            List<CompanyDataCenter> mappings = createCompanyDataCenterMappings(companies, dataCenters, members);
            log.info("✅ {} 개의 회사-전산실 매핑 생성 완료", mappings.size());

            // 7. 랙 데이터 생성
            List<Rack> racks = createRacks(dataCenters, members);
            log.info("✅ {} 개의 랙 생성 완료", racks.size());

            // 8. 장비 데이터 생성
            List<Equipment> equipments = createEquipments(racks, members);
            log.info("✅ {} 개의 장비 생성 완료", equipments.size());

            // 9. 장치 타입 생성
            List<DeviceType> deviceTypes = createDeviceTypes();
            log.info("✅ {} 개의 장치 타입 생성 완료", deviceTypes.size());

            // 10. 장치 데이터 생성
            List<Device> devices = createDevices(dataCenters, deviceTypes, racks, members);
            log.info("✅ {} 개의 장치 생성 완료", devices.size());

            log.info("=".repeat(80));
            log.info("🎉 초기 더미 데이터 로딩 완료!");
            printTestAccounts(members);
            log.info("=".repeat(80));

        } catch (DataIntegrityViolationException e) {
            // 중복 키 에러는 무시 (이미 데이터 존재)
            log.warn("⚠️ 중복 데이터 발견 - 초기화를 건너뜁니다: {}", e.getMessage());
        } catch (Exception e) {
            // 다른 에러는 로그만 남기고 앱은 계속 실행
            log.error("❌ 초기 데이터 로딩 중 오류 발생 (앱은 계속 실행됩니다): {}", e.getMessage());
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
                    .userName("admin" + userIndex)
                    .password(password)
                    .name("관리자" + userIndex)
                    .email("admin" + userIndex + "@" + company.getCode().toLowerCase() + ".com")
                    .phone("010-1000-" + String.format("%04d", userIndex))
                    .department("경영지원팀")
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
                    .userName("operator" + userIndex)
                    .password(password)
                    .name("운영자" + userIndex)
                    .email("operator" + userIndex + "@" + company.getCode().toLowerCase() + ".com")
                    .phone("010-2000-" + String.format("%04d", userIndex))
                    .department("운영팀")
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
                    .userName("viewer" + userIndex)
                    .password(password)
                    .name("조회자" + userIndex)
                    .email("viewer" + userIndex + "@" + company.getCode().toLowerCase() + ".com")
                    .phone("010-3000-" + String.format("%04d", userIndex))
                    .department("관리팀")
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

    private List<Department> createDepartments(List<Company> companies, List<Member> members) {
        List<Department> departments = new ArrayList<>();

        // COMP001 (테스트 회사) 부서들
        Company comp1 = companies.get(0);
        String creator1 = members.get(0).getUserName(); // admin1

        departments.add(Department.builder()
                .departmentCode("DEV")
                .departmentName("개발팀")
                .description("소프트웨어 개발 및 유지보수")
                .location("서울시 강남구 테헤란로 123, 5층")
                .phone("02-1234-5601")
                .email("dev@comp001.com")
                .employeeCount(0)
                .company(comp1)
                .createdBy(creator1)
                .build());

        departments.add(Department.builder()
                .departmentCode("OPS")
                .departmentName("운영팀")
                .description("시스템 운영 및 인프라 관리")
                .location("서울시 강남구 테헤란로 123, 3층")
                .phone("02-1234-5602")
                .email("ops@comp001.com")
                .employeeCount(0)
                .company(comp1)
                .createdBy(creator1)
                .build());

        departments.add(Department.builder()
                .departmentCode("IT")
                .departmentName("IT지원팀")
                .description("IT 인프라 및 헬프데스크")
                .location("서울시 강남구 테헤란로 123, 3층")
                .phone("02-1234-5603")
                .email("it@comp001.com")
                .employeeCount(0)
                .company(comp1)
                .createdBy(creator1)
                .build());

        departments.add(Department.builder()
                .departmentCode("MGMT")
                .departmentName("경영지원팀")
                .description("경영 기획 및 행정 지원")
                .location("서울시 강남구 테헤란로 123, 7층")
                .phone("02-1234-5604")
                .email("mgmt@comp001.com")
                .employeeCount(0)
                .company(comp1)
                .createdBy(creator1)
                .build());

        // COMP002 (데이터센터 운영사) 부서들
        Company comp2 = companies.get(1);
        String creator2 = members.get(3).getUserName(); // admin2

        departments.add(Department.builder()
                .departmentCode("DC_OPS")
                .departmentName("전산실운영팀")
                .description("데이터센터 시설 운영 및 관리")
                .location("서울시 서초구 반포대로 456, 2층")
                .phone("02-2345-6701")
                .email("dcops@comp002.com")
                .employeeCount(0)
                .company(comp2)
                .createdBy(creator2)
                .build());

        departments.add(Department.builder()
                .departmentCode("INFRA")
                .departmentName("인프라관리팀")
                .description("네트워크 및 서버 인프라 관리")
                .location("서울시 서초구 반포대로 456, 3층")
                .phone("02-2345-6702")
                .email("infra@comp002.com")
                .employeeCount(0)
                .company(comp2)
                .createdBy(creator2)
                .build());

        departments.add(Department.builder()
                .departmentCode("SECURITY")
                .departmentName("보안관리팀")
                .description("물리 및 사이버 보안 관리")
                .location("서울시 서초구 반포대로 456, 1층")
                .phone("02-2345-6703")
                .email("security@comp002.com")
                .employeeCount(0)
                .company(comp2)
                .createdBy(creator2)
                .build());

        departments.add(Department.builder()
                .departmentCode("FACILITY")
                .departmentName("시설관리팀")
                .description("전력, 냉각, 공조 시설 관리")
                .location("서울시 서초구 반포대로 456, 지하1층")
                .phone("02-2345-6704")
                .email("facility@comp002.com")
                .employeeCount(0)
                .company(comp2)
                .createdBy(creator2)
                .build());

        // COMP003 (클라우드 솔루션) 부서들
        Company comp3 = companies.get(2);
        String creator3 = members.get(6).getUserName(); // admin3

        departments.add(Department.builder()
                .departmentCode("CLOUD")
                .departmentName("클라우드서비스팀")
                .description("클라우드 플랫폼 개발 및 운영")
                .location("서울시 송파구 올림픽로 789, 10층")
                .phone("02-3456-7801")
                .email("cloud@comp003.com")
                .employeeCount(0)
                .company(comp3)
                .createdBy(creator3)
                .build());

        departments.add(Department.builder()
                .departmentCode("DEVOPS")
                .departmentName("DevOps팀")
                .description("CI/CD 파이프라인 및 자동화")
                .location("서울시 송파구 올림픽로 789, 9층")
                .phone("02-3456-7802")
                .email("devops@comp003.com")
                .employeeCount(0)
                .company(comp3)
                .createdBy(creator3)
                .build());

        departments.add(Department.builder()
                .departmentCode("PLATFORM")
                .departmentName("플랫폼개발팀")
                .description("클라우드 플랫폼 핵심 기능 개발")
                .location("서울시 송파구 올림픽로 789, 8층")
                .phone("02-3456-7803")
                .email("platform@comp003.com")
                .employeeCount(0)
                .company(comp3)
                .createdBy(creator3)
                .build());

        departments.add(Department.builder()
                .departmentCode("CS")
                .departmentName("고객지원팀")
                .description("고객 문의 및 기술 지원")
                .location("서울시 송파구 올림픽로 789, 6층")
                .phone("02-3456-7804")
                .email("cs@comp003.com")
                .employeeCount(0)
                .company(comp3)
                .createdBy(creator3)
                .build());

        return departmentRepository.saveAll(departments);
    }

    private List<MemberDepartment> createMemberDepartmentMappings(
            List<Member> members,
            List<Department> departments) {

        List<MemberDepartment> mappings = new ArrayList<>();

        // COMP001 회원들을 부서에 배정
        // admin1 -> 경영지원팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(0))
                .department(departments.get(3)) // 경영지원팀
                .isPrimary(true)
                .position("팀장")
                .joinDate(LocalDate.of(2024, 1, 1))
                .createdBy(members.get(0).getUserName())
                .build());

        // operator1 -> 운영팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(1))
                .department(departments.get(1)) // 운영팀
                .isPrimary(true)
                .position("선임")
                .joinDate(LocalDate.of(2024, 1, 10))
                .createdBy(members.get(0).getUserName())
                .build());

        // viewer1 -> IT지원팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(2))
                .department(departments.get(2)) // IT지원팀
                .isPrimary(true)
                .position("사원")
                .joinDate(LocalDate.of(2024, 2, 1))
                .createdBy(members.get(0).getUserName())
                .build());

        // COMP002 회원들을 부서에 배정
        // admin2 -> 전산실운영팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(3))
                .department(departments.get(4)) // 전산실운영팀
                .isPrimary(true)
                .position("본부장")
                .joinDate(LocalDate.of(2023, 6, 15))
                .createdBy(members.get(3).getUserName())
                .build());

        // operator2 -> 인프라관리팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(4))
                .department(departments.get(5)) // 인프라관리팀
                .isPrimary(true)
                .position("과장")
                .joinDate(LocalDate.of(2023, 7, 1))
                .createdBy(members.get(3).getUserName())
                .build());

        // viewer2 -> 보안관리팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(5))
                .department(departments.get(6)) // 보안관리팀
                .isPrimary(true)
                .position("대리")
                .joinDate(LocalDate.of(2023, 8, 1))
                .createdBy(members.get(3).getUserName())
                .build());

        // COMP003 회원들을 부서에 배정
        // admin3 -> 클라우드서비스팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(6))
                .department(departments.get(8)) // 클라우드서비스팀
                .isPrimary(true)
                .position("이사")
                .joinDate(LocalDate.of(2022, 3, 20))
                .createdBy(members.get(6).getUserName())
                .build());

        // operator3 -> DevOps팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(7))
                .department(departments.get(9)) // DevOps팀
                .isPrimary(true)
                .position("책임")
                .joinDate(LocalDate.of(2022, 5, 1))
                .createdBy(members.get(6).getUserName())
                .build());

        // viewer3 -> 고객지원팀 (주부서)
        mappings.add(MemberDepartment.builder()
                .member(members.get(8))
                .department(departments.get(11)) // 고객지원팀
                .isPrimary(true)
                .position("주임")
                .joinDate(LocalDate.of(2023, 1, 1))
                .createdBy(members.get(6).getUserName())
                .build());

        // 매핑 저장
        List<MemberDepartment> savedMappings = memberDepartmentRepository.saveAll(mappings);

        // 각 부서의 직원 수 증가
        for (MemberDepartment mapping : savedMappings) {
            mapping.getDepartment().incrementEmployeeCount();
        }

        // 부서 업데이트
        departmentRepository.saveAll(departments);

        return savedMappings;
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
                .floor(3)
                .rows(10)
                .columns(20)
                .status(DataCenterStatus.ACTIVE)
                .description("서울 메인 데이터센터")
                .totalArea(new BigDecimal("1000.50"))
                .totalPowerCapacity(new BigDecimal("2000.00"))
                .totalCoolingCapacity(new BigDecimal("1500.00"))
                .maxRackCount(200)
                .currentRackCount(0)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager1)
                .build());

        dataCenters.add(DataCenter.builder()
                .name("서울 제2전산실")
                .code("DC002")
                .location("서울시 금천구 가산디지털로 200")
                .floor(5)
                .rows(8)
                .columns(15)
                .status(DataCenterStatus.ACTIVE)
                .description("서울 백업 데이터센터")
                .totalArea(new BigDecimal("800.00"))
                .totalPowerCapacity(new BigDecimal("1500.00"))
                .totalCoolingCapacity(new BigDecimal("1200.00"))
                .maxRackCount(120)
                .currentRackCount(0)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager1)
                .build());

        dataCenters.add(DataCenter.builder()
                .name("부산 전산실")
                .code("DC003")
                .location("부산시 해운대구 센텀로 100")
                .floor(2)
                .rows(6)
                .columns(12)
                .status(DataCenterStatus.ACTIVE)
                .description("부산 지역 데이터센터")
                .totalArea(new BigDecimal("600.00"))
                .totalPowerCapacity(new BigDecimal("1000.00"))
                .totalCoolingCapacity(new BigDecimal("800.00"))
                .maxRackCount(72)
                .currentRackCount(0)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager2)
                .build());

        dataCenters.add(DataCenter.builder()
                .name("대전 전산실")
                .code("DC004")
                .location("대전시 유성구 테크노로 50")
                .floor(1)
                .rows(5)
                .columns(10)
                .status(DataCenterStatus.MAINTENANCE)
                .description("대전 연구단지 데이터센터 (점검중)")
                .totalArea(new BigDecimal("500.00"))
                .totalPowerCapacity(new BigDecimal("800.00"))
                .totalCoolingCapacity(new BigDecimal("600.00"))
                .maxRackCount(50)
                .currentRackCount(0)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager2)
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
                .grantedBy(members.get(0).getUserName())
                .build());

        mappings.add(CompanyDataCenter.builder()
                .company(companies.get(0))
                .dataCenter(dataCenters.get(1))
                .description("백업 전산실 사용 계약")
                .grantedBy(members.get(0).getUserName())
                .build());

        // COMP002 (데이터센터 운영사) -> 모든 DC 접근 가능
        for (DataCenter dc : dataCenters) {
            mappings.add(CompanyDataCenter.builder()
                    .company(companies.get(1))
                    .dataCenter(dc)
                    .description("전산실 운영사 - 전체 접근 권한")
                    .grantedBy(members.get(3).getUserName())
                    .build());
        }

        // COMP003 (클라우드 솔루션) -> DC001, DC003 접근 가능
        mappings.add(CompanyDataCenter.builder()
                .company(companies.get(2))
                .dataCenter(dataCenters.get(0))
                .description("서울 메인 전산실 사용")
                .grantedBy(members.get(0).getUserName())
                .build());

        mappings.add(CompanyDataCenter.builder()
                .company(companies.get(2))
                .dataCenter(dataCenters.get(2))
                .description("부산 전산실 사용")
                .grantedBy(members.get(3).getUserName())
                .build());

        return companyDataCenterRepository.saveAll(mappings);
    }

    private List<Rack> createRacks(List<DataCenter> dataCenters, List<Member> members) {
        List<Rack> racks = new ArrayList<>();
        Member manager1 = members.get(0);
        Member manager2 = members.get(3);

        // DC001에 10개의 랙 생성
        DataCenter dc1 = dataCenters.get(0);
        for (int i = 1; i <= 10; i++) {
            Rack rack = Rack.builder()
                    .rackName("Rack-A" + String.format("%02d", i))
                    .groupNumber("A-GROUP")
                    .rackLocation("Row-1, Col-" + i)
                    .totalUnits(42)
                    .usedUnits(0)
                    .availableUnits(42)
                    .doorDirection(DoorDirection.FRONT)
                    .zoneDirection(ZoneDirection.EAST)
                    .width(new BigDecimal("60.0"))
                    .depth(new BigDecimal("100.0"))
                    .department("IT운영팀")
                    .maxPowerCapacity(new BigDecimal("10.0"))
                    .currentPowerUsage(BigDecimal.ZERO)
                    .measuredPower(BigDecimal.ZERO)
                    .maxWeightCapacity(new BigDecimal("1000.0"))
                    .currentWeight(BigDecimal.ZERO)
                    .manufacturer("APC")
                    .serialNumber("APC-" + dc1.getCode() + "-" + i)
                    .managementNumber("MNG-" + i)
                    .status(RackStatus.ACTIVE)
                    .rackType(RackType.STANDARD)
                    .colorCode("#4A90E2")
                    .notes("서울 제1전산실 A그룹 랙")
                    .managerId(manager1.getId())
                    .datacenter(dc1)
                    .createdBy(manager1.getUserName())
                    .build();

            racks.add(rack);
            dc1.incrementRackCount();
        }

        // DC002에 5개의 랙 생성
        DataCenter dc2 = dataCenters.get(1);
        for (int i = 1; i <= 5; i++) {
            Rack rack = Rack.builder()
                    .rackName("Rack-B" + String.format("%02d", i))
                    .groupNumber("B-GROUP")
                    .rackLocation("Row-2, Col-" + i)
                    .totalUnits(42)
                    .usedUnits(0)
                    .availableUnits(42)
                    .doorDirection(DoorDirection.FRONT)
                    .zoneDirection(ZoneDirection.WEST)
                    .width(new BigDecimal("60.0"))
                    .depth(new BigDecimal("100.0"))
                    .department("백업운영팀")
                    .maxPowerCapacity(new BigDecimal("10.0"))
                    .currentPowerUsage(BigDecimal.ZERO)
                    .measuredPower(BigDecimal.ZERO)
                    .maxWeightCapacity(new BigDecimal("1000.0"))
                    .currentWeight(BigDecimal.ZERO)
                    .manufacturer("Dell")
                    .serialNumber("DELL-" + dc2.getCode() + "-" + i)
                    .managementNumber("MNG-" + (10 + i))
                    .status(RackStatus.ACTIVE)
                    .rackType(RackType.STANDARD)
                    .colorCode("#50C878")
                    .notes("서울 제2전산실 B그룹 랙")
                    .managerId(manager1.getId())
                    .datacenter(dc2)
                    .createdBy(manager1.getUserName())
                    .build();

            racks.add(rack);
            dc2.incrementRackCount();
        }

        // DC003에 3개의 랙 생성
        DataCenter dc3 = dataCenters.get(2);
        for (int i = 1; i <= 3; i++) {
            Rack rack = Rack.builder()
                    .rackName("Rack-C" + String.format("%02d", i))
                    .groupNumber("C-GROUP")
                    .rackLocation("Row-1, Col-" + i)
                    .totalUnits(42)
                    .usedUnits(0)
                    .availableUnits(42)
                    .doorDirection(DoorDirection.FRONT)
                    .zoneDirection(ZoneDirection.SOUTH)
                    .width(new BigDecimal("60.0"))
                    .depth(new BigDecimal("100.0"))
                    .department("부산운영팀")
                    .maxPowerCapacity(new BigDecimal("10.0"))
                    .currentPowerUsage(BigDecimal.ZERO)
                    .measuredPower(BigDecimal.ZERO)
                    .maxWeightCapacity(new BigDecimal("1000.0"))
                    .currentWeight(BigDecimal.ZERO)
                    .manufacturer("HP")
                    .serialNumber("HP-" + dc3.getCode() + "-" + i)
                    .managementNumber("MNG-" + (20 + i))
                    .status(RackStatus.ACTIVE)
                    .rackType(RackType.STANDARD)
                    .colorCode("#FFD700")
                    .notes("부산 전산실 C그룹 랙")
                    .managerId(manager2.getId())
                    .datacenter(dc3)
                    .createdBy(manager2.getUserName())
                    .build();

            racks.add(rack);
            dc3.incrementRackCount();
        }

        List<Rack> savedRacks = rackRepository.saveAll(racks);
        dataCenterRepository.saveAll(List.of(dc1, dc2, dc3));

        return savedRacks;
    }

    private List<Equipment> createEquipments(List<Rack> racks, List<Member> members) {
        List<Equipment> equipments = new ArrayList<>();
        Member manager1 = members.get(0);

        int equipmentCounter = 1;

        // 각 랙마다 장비 배치 (총 10개 랙)
        for (int rackIdx = 0; rackIdx < Math.min(10, racks.size()); rackIdx++) {
            Rack rack = racks.get(rackIdx);
            int currentUnit = 1; // 랙 하단부터 시작

            // === 1. PDU (전력 분배) - 랙당 2개 (상단/하단) ===
            equipments.add(Equipment.builder()
                    .name(rack.getRackName() + "-PDU-TOP")
                    .code("PDU-" + String.format("%03d", equipmentCounter++))
                    .type(EquipmentType.PDU)
                    .startUnit(40)
                    .unitSize(2)
                    .positionType(EquipmentPositionType.NORMAL)
                    .modelName("APC Rack PDU 2G")
                    .manufacturer("APC")
                    .serialNumber("PDU-" + rack.getRackName() + "-TOP")
                    .ipAddress("192.168.100." + (10 + rackIdx * 10))
                    .powerConsumption(BigDecimal.ZERO)
                    .weight(new BigDecimal("4.5"))
                    .status(EquipmentStatus.NORMAL)
                    .installationDate(LocalDate.of(2024, 1, 10))
                    .notes("상단 전력 분배 장치")
                    .managerId(manager1.getId())
                    .rack(rack)
                    .position(40)
                    .height(2)
                    .build());

            equipments.add(Equipment.builder()
                    .name(rack.getRackName() + "-PDU-BOTTOM")
                    .code("PDU-" + String.format("%03d", equipmentCounter++))
                    .type(EquipmentType.PDU)
                    .startUnit(1)
                    .unitSize(1)
                    .positionType(EquipmentPositionType.NORMAL)
                    .modelName("APC Rack PDU 2G")
                    .manufacturer("APC")
                    .serialNumber("PDU-" + rack.getRackName() + "-BTM")
                    .ipAddress("192.168.100." + (11 + rackIdx * 10))
                    .powerConsumption(BigDecimal.ZERO)
                    .weight(new BigDecimal("3.5"))
                    .status(EquipmentStatus.NORMAL)
                    .installationDate(LocalDate.of(2024, 1, 10))
                    .notes("하단 전력 분배 장치")
                    .managerId(manager1.getId())
                    .rack(rack)
                    .position(1)
                    .height(1)
                    .build());

            currentUnit = 2;

            // === 2. 랙별 장비 구성 (타입별로 다르게) ===
            if (rackIdx < 5) {
                // Rack 0~4: 서버 중심 랙

                // 스위치 1개
                equipments.add(Equipment.builder()
                        .name(rack.getRackName() + "-SWITCH")
                        .code("SW-" + String.format("%03d", equipmentCounter++))
                        .type(EquipmentType.SWITCH)
                        .startUnit(currentUnit)
                        .unitSize(1)
                        .positionType(EquipmentPositionType.NORMAL)
                        .modelName("Cisco Catalyst 2960X")
                        .manufacturer("Cisco")
                        .serialNumber("SW-" + rack.getRackName())
                        .ipAddress("192.168.1." + (100 + rackIdx))
                        .macAddress(String.format("00:1A:2B:3C:%02d:01", rackIdx))
                        .powerConsumption(new BigDecimal("50.0"))
                        .weight(new BigDecimal("4.2"))
                        .status(EquipmentStatus.NORMAL)
                        .installationDate(LocalDate.of(2024, 1, 15))
                        .notes("Top of Rack 스위치")
                        .managerId(manager1.getId())
                        .rack(rack)
                        .position(currentUnit)
                        .height(1)
                        .build());
                currentUnit += 1;

                // 서버 3~4개
                int serverCount = 3 + (rackIdx % 2);
                for (int sIdx = 0; sIdx < serverCount; sIdx++) {
                    equipments.add(Equipment.builder()
                            .name(rack.getRackName() + "-SERVER-" + (sIdx + 1))
                            .code("SRV-" + String.format("%03d", equipmentCounter++))
                            .type(EquipmentType.SERVER)
                            .startUnit(currentUnit)
                            .unitSize(2)
                            .positionType(EquipmentPositionType.NORMAL)
                            .modelName("Dell PowerEdge R750")
                            .manufacturer("Dell")
                            .serialNumber("SRV-" + rack.getRackName() + "-" + (sIdx + 1))
                            .ipAddress("10.0." + rackIdx + "." + (10 + sIdx))
                            .macAddress(String.format("AA:BB:CC:DD:%02d:%02d", rackIdx, sIdx))
                            .os("Ubuntu 22.04 LTS")
                            .cpuSpec("Intel Xeon Silver 4314 16C 32T")
                            .memorySpec("128GB DDR4 ECC")
                            .diskSpec("SSD 1TB NVMe x 2")
                            .powerConsumption(new BigDecimal("450.0"))
                            .weight(new BigDecimal("28.5"))
                            .status(EquipmentStatus.NORMAL)
                            .installationDate(LocalDate.of(2024, 1, 15))
                            .notes("웹 서버 " + (sIdx + 1))
                            .managerId(manager1.getId())
                            .rack(rack)
                            .position(currentUnit)
                            .height(2)
                            .build());
                    currentUnit += 2;
                }

                // KVM (일부 랙에만)
                if (rackIdx % 2 == 0) {
                    equipments.add(Equipment.builder()
                            .name(rack.getRackName() + "-KVM")
                            .code("KVM-" + String.format("%03d", equipmentCounter++))
                            .type(EquipmentType.KVM)
                            .startUnit(currentUnit)
                            .unitSize(1)
                            .positionType(EquipmentPositionType.NORMAL)
                            .modelName("Raritan Dominion KX III")
                            .manufacturer("Raritan")
                            .serialNumber("KVM-" + rack.getRackName())
                            .ipAddress("192.168.2." + (10 + rackIdx))
                            .powerConsumption(new BigDecimal("25.0"))
                            .weight(new BigDecimal("3.0"))
                            .status(EquipmentStatus.NORMAL)
                            .installationDate(LocalDate.of(2024, 1, 12))
                            .notes("콘솔 스위치")
                            .managerId(manager1.getId())
                            .rack(rack)
                            .position(currentUnit)
                            .height(1)
                            .build());
                    currentUnit += 1;
                }

            } else if (rackIdx >= 5 && rackIdx < 8) {
                // Rack 5~7: 네트워크/스토리지 랙

                // 라우터
                equipments.add(Equipment.builder()
                        .name(rack.getRackName() + "-ROUTER")
                        .code("RTR-" + String.format("%03d", equipmentCounter++))
                        .type(EquipmentType.ROUTER)
                        .startUnit(currentUnit)
                        .unitSize(2)
                        .positionType(EquipmentPositionType.NORMAL)
                        .modelName("Cisco ISR 4451")
                        .manufacturer("Cisco")
                        .serialNumber("RTR-" + rack.getRackName())
                        .ipAddress("192.168.254." + rackIdx)
                        .macAddress(String.format("00:1B:2C:3D:%02d:00", rackIdx))
                        .powerConsumption(new BigDecimal("150.0"))
                        .weight(new BigDecimal("12.5"))
                        .status(EquipmentStatus.NORMAL)
                        .installationDate(LocalDate.of(2024, 1, 15))
                        .notes("코어 라우터")
                        .managerId(manager1.getId())
                        .rack(rack)
                        .position(currentUnit)
                        .height(2)
                        .build());
                currentUnit += 2;

                // 스위치 2개
                for (int swIdx = 0; swIdx < 2; swIdx++) {
                    equipments.add(Equipment.builder()
                            .name(rack.getRackName() + "-SWITCH-" + (swIdx + 1))
                            .code("SW-" + String.format("%03d", equipmentCounter++))
                            .type(EquipmentType.SWITCH)
                            .startUnit(currentUnit)
                            .unitSize(1)
                            .positionType(EquipmentPositionType.NORMAL)
                            .modelName("Cisco Catalyst 9300")
                            .manufacturer("Cisco")
                            .serialNumber("SW-" + rack.getRackName() + "-" + (swIdx + 1))
                            .ipAddress("192.168.1." + (150 + rackIdx * 10 + swIdx))
                            .macAddress(String.format("00:1A:2B:3C:%02d:%02d", rackIdx, swIdx))
                            .powerConsumption(new BigDecimal("120.0"))
                            .weight(new BigDecimal("8.5"))
                            .status(EquipmentStatus.NORMAL)
                            .installationDate(LocalDate.of(2024, 1, 15))
                            .notes("코어 스위치 " + (swIdx + 1))
                            .managerId(manager1.getId())
                            .rack(rack)
                            .position(currentUnit)
                            .height(1)
                            .build());
                    currentUnit += 1;
                }

                // 방화벽
                if (rackIdx == 5 || rackIdx == 6) {
                    equipments.add(Equipment.builder()
                            .name(rack.getRackName() + "-FIREWALL")
                            .code("FW-" + String.format("%03d", equipmentCounter++))
                            .type(EquipmentType.FIREWALL)
                            .startUnit(currentUnit)
                            .unitSize(1)
                            .positionType(EquipmentPositionType.NORMAL)
                            .modelName("Fortinet FortiGate 600E")
                            .manufacturer("Fortinet")
                            .serialNumber("FW-" + rack.getRackName())
                            .ipAddress("192.168.253." + rackIdx)
                            .macAddress(String.format("00:09:0F:09:%02d:00", rackIdx))
                            .powerConsumption(new BigDecimal("200.0"))
                            .weight(new BigDecimal("11.0"))
                            .status(EquipmentStatus.NORMAL)
                            .installationDate(LocalDate.of(2024, 1, 15))
                            .notes("경계 방화벽")
                            .managerId(manager1.getId())
                            .rack(rack)
                            .position(currentUnit)
                            .height(1)
                            .build());
                    currentUnit += 1;
                }

                // 로드밸런서
                if (rackIdx == 7) {
                    equipments.add(Equipment.builder()
                            .name(rack.getRackName() + "-LOAD-BALANCER")
                            .code("LB-" + String.format("%03d", equipmentCounter++))
                            .type(EquipmentType.LOAD_BALANCER)
                            .startUnit(currentUnit)
                            .unitSize(1)
                            .positionType(EquipmentPositionType.NORMAL)
                            .modelName("F5 BIG-IP 4000s")
                            .manufacturer("F5 Networks")
                            .serialNumber("LB-" + rack.getRackName())
                            .ipAddress("192.168.252." + rackIdx)
                            .macAddress(String.format("F5:F5:F5:F5:%02d:00", rackIdx))
                            .powerConsumption(new BigDecimal("180.0"))
                            .weight(new BigDecimal("15.0"))
                            .status(EquipmentStatus.NORMAL)
                            .installationDate(LocalDate.of(2024, 1, 15))
                            .notes("L7 로드밸런서")
                            .managerId(manager1.getId())
                            .rack(rack)
                            .position(currentUnit)
                            .height(1)
                            .build());
                    currentUnit += 1;
                }

                // 스토리지 1~2개
                int storageCount = (rackIdx == 5) ? 2 : 1;
                for (int stIdx = 0; stIdx < storageCount; stIdx++) {
                    equipments.add(Equipment.builder()
                            .name(rack.getRackName() + "-STORAGE-" + (stIdx + 1))
                            .code("STG-" + String.format("%03d", equipmentCounter++))
                            .type(EquipmentType.STORAGE)
                            .startUnit(currentUnit)
                            .unitSize(4)
                            .positionType(EquipmentPositionType.NORMAL)
                            .modelName("NetApp FAS2750")
                            .manufacturer("NetApp")
                            .serialNumber("STG-" + rack.getRackName() + "-" + (stIdx + 1))
                            .ipAddress("10.10." + rackIdx + "." + (10 + stIdx))
                            .macAddress(String.format("00:A0:98:00:%02d:%02d", rackIdx, stIdx))
                            .diskSpec("24 x 8TB SAS HDD")
                            .powerConsumption(new BigDecimal("800.0"))
                            .weight(new BigDecimal("45.0"))
                            .status(EquipmentStatus.NORMAL)
                            .installationDate(LocalDate.of(2024, 1, 15))
                            .notes("통합 스토리지 " + (stIdx + 1))
                            .managerId(manager1.getId())
                            .rack(rack)
                            .position(currentUnit)
                            .height(4)
                            .build());
                    currentUnit += 4;
                }

            } else {
                // Rack 8~9: 혼합 랙

                // 스위치
                equipments.add(Equipment.builder()
                        .name(rack.getRackName() + "-SWITCH")
                        .code("SW-" + String.format("%03d", equipmentCounter++))
                        .type(EquipmentType.SWITCH)
                        .startUnit(currentUnit)
                        .unitSize(1)
                        .positionType(EquipmentPositionType.NORMAL)
                        .modelName("HP Aruba 2930F")
                        .manufacturer("HPE")
                        .serialNumber("SW-" + rack.getRackName())
                        .ipAddress("192.168.1." + (200 + rackIdx))
                        .macAddress(String.format("00:1A:2B:3C:%02d:01", rackIdx))
                        .powerConsumption(new BigDecimal("60.0"))
                        .weight(new BigDecimal("5.0"))
                        .status(EquipmentStatus.NORMAL)
                        .installationDate(LocalDate.of(2024, 1, 15))
                        .notes("엣지 스위치")
                        .managerId(manager1.getId())
                        .rack(rack)
                        .position(currentUnit)
                        .height(1)
                        .build());
                currentUnit += 1;

                // 서버 2개
                for (int sIdx = 0; sIdx < 2; sIdx++) {
                    equipments.add(Equipment.builder()
                            .name(rack.getRackName() + "-SERVER-" + (sIdx + 1))
                            .code("SRV-" + String.format("%03d", equipmentCounter++))
                            .type(EquipmentType.SERVER)
                            .startUnit(currentUnit)
                            .unitSize(2)
                            .positionType(EquipmentPositionType.NORMAL)
                            .modelName("HPE ProLiant DL380 Gen10")
                            .manufacturer("HPE")
                            .serialNumber("SRV-" + rack.getRackName() + "-" + (sIdx + 1))
                            .ipAddress("10.0." + rackIdx + "." + (10 + sIdx))
                            .macAddress(String.format("AA:BB:CC:DD:%02d:%02d", rackIdx, sIdx))
                            .os("Windows Server 2022")
                            .cpuSpec("Intel Xeon Gold 5218 16C 32T")
                            .memorySpec("256GB DDR4 ECC")
                            .diskSpec("SSD 2TB NVMe x 4")
                            .powerConsumption(new BigDecimal("550.0"))
                            .weight(new BigDecimal("32.0"))
                            .status(EquipmentStatus.NORMAL)
                            .installationDate(LocalDate.of(2024, 1, 15))
                            .notes("애플리케이션 서버 " + (sIdx + 1))
                            .managerId(manager1.getId())
                            .rack(rack)
                            .position(currentUnit)
                            .height(2)
                            .build());
                    currentUnit += 2;
                }

                // 스토리지
                equipments.add(Equipment.builder()
                        .name(rack.getRackName() + "-STORAGE")
                        .code("STG-" + String.format("%03d", equipmentCounter++))
                        .type(EquipmentType.STORAGE)
                        .startUnit(currentUnit)
                        .unitSize(3)
                        .positionType(EquipmentPositionType.NORMAL)
                        .modelName("QNAP TS-1277XU-RP")
                        .manufacturer("QNAP")
                        .serialNumber("STG-" + rack.getRackName())
                        .ipAddress("10.10." + rackIdx + ".10")
                        .diskSpec("12 x 4TB NVMe SSD")
                        .powerConsumption(new BigDecimal("350.0"))
                        .weight(new BigDecimal("25.0"))
                        .status(EquipmentStatus.NORMAL)
                        .installationDate(LocalDate.of(2024, 1, 15))
                        .notes("백업 스토리지")
                        .managerId(manager1.getId())
                        .rack(rack)
                        .position(currentUnit)
                        .height(3)
                        .build());
                currentUnit += 3;
            }

            // === 3. 온습도 센서 (모든 랙 상단에 1개씩) ===
            equipments.add(Equipment.builder()
                    .name(rack.getRackName() + "-ENV-SENSOR")
                    .code("ENV-" + String.format("%03d", equipmentCounter++))
                    .type(EquipmentType.ENVIRONMENTAL_SENSOR)
                    .startUnit(42)
                    .unitSize(0) // 랙 유닛을 차지하지 않음
                    .positionType(EquipmentPositionType.NORMAL)
                    .modelName("Kentix MultiSensor-LAN")
                    .manufacturer("Kentix")
                    .serialNumber("ENV-" + rack.getRackName())
                    .ipAddress("192.168.50." + (10 + rackIdx))
                    .powerConsumption(new BigDecimal("5.0"))
                    .weight(new BigDecimal("0.3"))
                    .status(EquipmentStatus.NORMAL)
                    .installationDate(LocalDate.of(2024, 1, 10))
                    .notes("온습도 모니터링 센서")
                    .managerId(manager1.getId())
                    .rack(rack)
                    .position(42)
                    .height(0)
                    .build());
        }

        List<Equipment> savedEquipments = equipmentRepository.saveAll(equipments);

        // 랙 사용률 업데이트
        for (int i = 0; i < Math.min(10, racks.size()); i++) {
            Rack rack = racks.get(i);
            List<Equipment> rackEquipments = savedEquipments.stream()
                    .filter(e -> e.getRack() != null && e.getRack().getId().equals(rack.getId()))
                    .filter(e -> e.getType() != EquipmentType.ENVIRONMENTAL_SENSOR) // 센서 제외
                    .toList();

            int totalUsedUnits = rackEquipments.stream()
                    .mapToInt(Equipment::getUnitSize)
                    .sum();

            BigDecimal totalPower = rackEquipments.stream()
                    .map(e -> e.getPowerConsumption() != null ? e.getPowerConsumption() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalWeight = rackEquipments.stream()
                    .map(e -> e.getWeight() != null ? e.getWeight() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            rack.setUsedUnits(totalUsedUnits);
            rack.setAvailableUnits(42 - totalUsedUnits);
            rack.setCurrentPowerUsage(totalPower);
            rack.setCurrentWeight(totalWeight);
        }

        rackRepository.saveAll(racks.subList(0, Math.min(10, racks.size())));

        return savedEquipments;
    }

    private List<DeviceType> createDeviceTypes() {
        List<DeviceType> deviceTypes = new ArrayList<>();

        deviceTypes.add(DeviceType.builder()
                .typeName("server")
                .category(DeviceCategory.NETWORK)
                .description("서버 랙")
                .iconUrl("/icons/server.svg")
                .attributesTemplate("{\"maxRacks\": 42, \"powerSupply\": \"redundant\"}")
                .build());

        deviceTypes.add(DeviceType.builder()
                .typeName("door")
                .category(DeviceCategory.SECURITY)
                .description("출입문")
                .iconUrl("/icons/door.svg")
                .attributesTemplate("{\"accessControl\": true, \"cardReader\": true}")
                .build());

        deviceTypes.add(DeviceType.builder()
                .typeName("climatic_chamber")
                .category(DeviceCategory.COOLING)
                .description("항온항습기")
                .iconUrl("/icons/climatic_chamber.svg")
                .attributesTemplate("{\"temperatureRange\": \"18-27°C\", \"humidityRange\": \"40-60%\"}")
                .build());

        deviceTypes.add(DeviceType.builder()
                .typeName("fire_extinguisher")
                .category(DeviceCategory.SAFETY)
                .description("소화기")
                .iconUrl("/icons/fire_extinguisher.svg")
                .attributesTemplate("{\"type\": \"CO2\", \"capacity\": \"10kg\"}")
                .build());

        deviceTypes.add(DeviceType.builder()
                .typeName("thermometer")
                .category(DeviceCategory.MONITORING)
                .description("온도 센서")
                .iconUrl("/icons/thermometer.svg")
                .attributesTemplate("{\"range\": \"-10~50°C\", \"accuracy\": \"±0.5°C\"}")
                .build());

        deviceTypes.add(DeviceType.builder()
                .typeName("aircon")
                .category(DeviceCategory.COOLING)
                .description("정밀 에어컨")
                .iconUrl("/icons/aircon.svg")
                .attributesTemplate("{\"coolingCapacity\": \"15kW\", \"efficiency\": \"A+++\"}")
                .build());

        return deviceTypeRepository.saveAll(deviceTypes);
    }

    private List<Device> createDevices(
            List<DataCenter> dataCenters,
            List<DeviceType> deviceTypes,
            List<Rack> racks,
            List<Member> members) {

        List<Device> devices = new ArrayList<>();
        Member manager1 = members.get(0);
        DataCenter dc1 = dataCenters.get(0);

        // DeviceType 맵핑
        DeviceType serverType = deviceTypes.stream()
                .filter(dt -> dt.getTypeName().equals("server")).findFirst().orElse(deviceTypes.get(0));
        DeviceType doorType = deviceTypes.stream()
                .filter(dt -> dt.getTypeName().equals("door")).findFirst().orElse(deviceTypes.get(1));
        DeviceType airconType = deviceTypes.stream()
                .filter(dt -> dt.getTypeName().equals("aircon")).findFirst().orElse(deviceTypes.get(5));
        DeviceType fireExtType = deviceTypes.stream()
                .filter(dt -> dt.getTypeName().equals("fire_extinguisher")).findFirst().orElse(deviceTypes.get(3));
        DeviceType thermoType = deviceTypes.stream()
                .filter(dt -> dt.getTypeName().equals("thermometer")).findFirst().orElse(deviceTypes.get(4));

        // 서버 랙 배치 (4개)
        for (int i = 0; i < 4 && i < racks.size(); i++) {
            devices.add(Device.builder()
                    .deviceName("A1-SERVER-" + (i + 1))
                    .deviceCode("A1-SERVER-" + (i + 1))
                    .gridX(2 + (i * 2))
                    .gridZ(0)
                    .rotation(0)
                    .status(DeviceStatus.NORMAL)
                    .modelName("Dell Server Rack")
                    .manufacturer("Dell")
                    .serialNumber("DSR-2024-" + (i + 1))
                    .purchaseDate(LocalDate.of(2024, 1, 15))
                    .warrantyEndDate(LocalDate.of(2029, 1, 15))
                    .notes("메인 서버 랙 " + (i + 1))
                    .deviceType(serverType)
                    .managerId(manager1.getId())
                    .datacenter(dc1)
                    .rack(racks.get(i))
                    .build());
        }

        // 출입문 (2개)
        devices.add(Device.builder()
                .deviceName("정문")
                .deviceCode("DOOR-MAIN-001")
                .gridX(5)
                .gridZ(0)
                .rotation(90)
                .status(DeviceStatus.NORMAL)
                .modelName("SecureDoor Pro")
                .manufacturer("SecureTech")
                .serialNumber("SD-2024-001")
                .purchaseDate(LocalDate.of(2024, 1, 10))
                .notes("메인 출입구")
                .deviceType(doorType)
                .managerId(manager1.getId())
                .datacenter(dc1)
                .build());

        devices.add(Device.builder()
                .deviceName("후문")
                .deviceCode("DOOR-REAR-001")
                .gridY(10)
                .gridX(15)
                .gridZ(0)
                .rotation(270)
                .status(DeviceStatus.NORMAL)
                .modelName("SecureDoor Pro")
                .manufacturer("SecureTech")
                .serialNumber("SD-2024-002")
                .purchaseDate(LocalDate.of(2024, 1, 10))
                .notes("비상 출구")
                .deviceType(doorType)
                .managerId(manager1.getId())
                .datacenter(dc1)
                .build());

        // 에어컨 (2개)
        devices.add(Device.builder()
                .deviceName("정밀에어컨-1")
                .deviceCode("AIRCON-001")
                .gridY(0)
                .gridX(0)
                .gridZ(0)
                .rotation(0)
                .status(DeviceStatus.NORMAL)
                .modelName("PrecisionAir 15kW")
                .manufacturer("CoolTech")
                .serialNumber("PA-15K-2024-001")
                .purchaseDate(LocalDate.of(2024, 1, 25))
                .warrantyEndDate(LocalDate.of(2029, 1, 25))
                .notes("좌측 냉각 시스템")
                .deviceType(airconType)
                .managerId(manager1.getId())
                .datacenter(dc1)
                .build());

        devices.add(Device.builder()
                .deviceName("정밀에어컨-2")
                .deviceCode("AIRCON-002")
                .gridY(0)
                .gridX(19)
                .gridZ(0)
                .rotation(180)
                .status(DeviceStatus.NORMAL)
                .modelName("PrecisionAir 15kW")
                .manufacturer("CoolTech")
                .serialNumber("PA-15K-2024-002")
                .purchaseDate(LocalDate.of(2024, 1, 25))
                .warrantyEndDate(LocalDate.of(2029, 1, 25))
                .notes("우측 냉각 시스템")
                .deviceType(airconType)
                .managerId(manager1.getId())
                .datacenter(dc1)
                .build());

        // 소화기 (4개 - 모서리)
        int[][] fireExtPositions = {{0, 0}, {0, 19}, {9, 0}, {9, 19}};
        for (int i = 0; i < fireExtPositions.length; i++) {
            devices.add(Device.builder()
                    .deviceName("소화기-" + (i + 1))
                    .deviceCode("FIRE-EXT-" + (i + 1))
                    .gridY(fireExtPositions[i][0])
                    .gridX(fireExtPositions[i][1])
                    .gridZ(0)
                    .rotation(0)
                    .status(DeviceStatus.NORMAL)
                    .modelName("CO2-10K")
                    .manufacturer("SafetyFirst")
                    .serialNumber("SF-CO2-2024-" + (i + 1))
                    .purchaseDate(LocalDate.of(2024, 1, 5))
                    .notes("모서리 배치 소화기")
                    .deviceType(fireExtType)
                    .managerId(manager1.getId())
                    .datacenter(dc1)
                    .build());
        }

        // 온도계 (5개)
        int[][] thermoPositions = {{2, 5}, {5, 5}, {2, 10}, {5, 10}, {2, 15}};
        for (int i = 0; i < thermoPositions.length; i++) {
            devices.add(Device.builder()
                    .deviceName("온도센서-" + (i + 1))
                    .deviceCode("TEMP-SENSOR-" + (i + 1))
                    .gridY(thermoPositions[i][0])
                    .gridX(thermoPositions[i][1])
                    .gridZ(0)
                    .rotation(0)
                    .status(DeviceStatus.NORMAL)
                    .modelName("TempSense Pro")
                    .manufacturer("SensorTech")
                    .serialNumber("TS-PRO-2024-" + (i + 1))
                    .purchaseDate(LocalDate.of(2024, 1, 20))
                    .notes("온도 모니터링 센서")
                    .deviceType(thermoType)
                    .managerId(manager1.getId())
                    .datacenter(dc1)
                    .build());
        }

        return deviceRepository.saveAll(devices);
    }

    private void printTestAccounts(List<Member> members) {
        log.info("");
        log.info("💡 테스트 계정 정보");
        log.info("-".repeat(80));
        log.info("   비밀번호: password123 (모든 계정 공통)");
        log.info("");
        log.info("   아이디          | 권한        | 회사");
        log.info("-".repeat(80));

        for (Member member : members) {
            log.info("   {:15} | {:11} | {}",
                    member.getUserName(),
                    member.getRole(),
                    member.getCompany().getName());
        }

        log.info("");
        log.info("📊 생성된 데이터 요약");
        log.info("-".repeat(80));
        log.info("   - 회사: 3개");
        log.info("   - 사용자: 9명 (각 회사당 ADMIN, OPERATOR, VIEWER)");
        log.info("   - 전산실: 4개");
        log.info("   - 랙: {}개", rackRepository.count());
        log.info("   - 장비: {}개", equipmentRepository.count());
        log.info("   - 장치 타입: {}개", deviceTypeRepository.count());
        log.info("   - 부서: {}개", departmentRepository.count());
        log.info("   - 사용자-부서 매핑: {}개", memberDepartmentRepository.count());
        log.info("   - 장치: {}개", deviceRepository.count());
        log.info("");
        log.info("");
    }
}