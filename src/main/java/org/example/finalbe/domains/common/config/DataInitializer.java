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

            // 5. 랙 데이터 생성
            List<Rack> racks = createRacks(dataCenters, members);
            log.info("✅ {} 개의 랙 생성 완료", racks.size());

            // 6. 장비 데이터 생성
            List<Equipment> equipments = createEquipments(racks, members);
            log.info("✅ {} 개의 장비 생성 완료", equipments.size());

            // 7. 장치 타입 생성
            List<DeviceType> deviceTypes = createDeviceTypes();
            log.info("✅ {} 개의 장치 타입 생성 완료", deviceTypes.size());

            // 8. 장치 데이터 생성
            List<Device> devices = createDevices(dataCenters, deviceTypes, racks, members);
            log.info("✅ {} 개의 장치 생성 완료", devices.size());

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
                    .userName("admin" + userIndex)
                    .password(password)
                    .name("관리자" + userIndex)
                    .email("admin" + userIndex + "@" + company.getCode().toLowerCase() + ".com")
                    .phone("010-1000-" + String.format("%04d", userIndex))
                    .department("경영지원팀")  // 부서 추가!
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
                    .department("운영팀")  // 부서 추가!
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
                    .department("관리팀")  // 부서 추가!
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
                .currentRackCount(0)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager1)
                .createdBy(manager1.getUserName())
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
                .currentRackCount(0)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager1)
                .createdBy(manager1.getUserName())
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
                .currentRackCount(0)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager2)
                .createdBy(manager2.getUserName())
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
                .currentRackCount(0)
                .temperatureMin(new BigDecimal("18.0"))
                .temperatureMax(new BigDecimal("27.0"))
                .humidityMin(new BigDecimal("40.0"))
                .humidityMax(new BigDecimal("60.0"))
                .manager(manager2)
                .createdBy(manager2.getUserName())
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
            // 랙을 추가할 때마다 카운트 증가
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
            // 랙을 추가할 때마다 카운트 증가
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
            // 랙을 추가할 때마다 카운트 증가
            dc3.incrementRackCount();
        }

        List<Rack> savedRacks = rackRepository.saveAll(racks);

        // 전산실 변경사항 저장 (incrementRackCount로 이미 증가된 상태)
        dataCenterRepository.saveAll(List.of(dc1, dc2, dc3));

        return savedRacks;
    }

    private List<Equipment> createEquipments(List<Rack> racks, List<Member> members) {
        List<Equipment> equipments = new ArrayList<>();
        Member manager1 = members.get(0);

        // 첫 3개 랙에 각각 3개의 장비 배치
        for (int rackIdx = 0; rackIdx < 3 && rackIdx < racks.size(); rackIdx++) {
            Rack rack = racks.get(rackIdx);

            for (int equipIdx = 1; equipIdx <= 3; equipIdx++) {
                int startUnit = (equipIdx - 1) * 10 + 1; // 1, 11, 21
                equipments.add(Equipment.builder()
                        .name(rack.getRackName() + "-서버-" + equipIdx)
                        .code(rack.getRackName() + "-SRV-" + equipIdx)
                        .type(EquipmentType.SERVER)
                        .startUnit(startUnit)
                        .unitSize(4)
                        .positionType(EquipmentPositionType.NORMAL)
                        .modelName("Dell PowerEdge R740")
                        .manufacturer("Dell")
                        .serialNumber("SN-" + rack.getRackName() + "-" + equipIdx)
                        .ipAddress("192.168.1." + ((rackIdx * 10) + equipIdx))
                        .macAddress(String.format("AA:BB:CC:DD:%02d:%02d", rackIdx, equipIdx))
                        .os("Ubuntu 22.04 LTS")
                        .cpuSpec("Intel Xeon Gold 6248R 24C 48T")
                        .memorySpec("128GB DDR4 ECC")
                        .diskSpec("SSD 2TB NVMe x 4")
                        .powerConsumption(new BigDecimal("750.0"))
                        .weight(new BigDecimal("35.5"))
                        .status(EquipmentStatus.NORMAL)
                        .imageUrl("https://example.com/server.png")
                        .installationDate(LocalDate.of(2024, 1, 15))
                        .notes("메인 서버 " + equipIdx)
                        .managerId(manager1.getId())
                        .rack(rack)
                        .position(startUnit)
                        .height(4)
                        .build());
            }
        }

        List<Equipment> savedEquipments = equipmentRepository.saveAll(equipments);

        // 랙의 사용 유닛 업데이트
        for (int i = 0; i < 3 && i < racks.size(); i++) {
            Rack rack = racks.get(i);
            rack.setUsedUnits(12); // 4U x 3개 = 12U
            rack.setAvailableUnits(30); // 42 - 12 = 30
            rack.setCurrentPowerUsage(new BigDecimal("2250.0")); // 750W x 3
            rack.setCurrentWeight(new BigDecimal("106.5")); // 35.5kg x 3
        }
        rackRepository.saveAll(racks.subList(0, Math.min(3, racks.size())));

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
        log.info("   - 장치: {}개", deviceRepository.count());
        log.info("");
    }
}