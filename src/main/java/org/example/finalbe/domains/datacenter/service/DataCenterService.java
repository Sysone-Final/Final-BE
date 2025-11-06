package org.example.finalbe.domains.datacenter.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.finalbe.domains.common.enumdir.DelYN;
import org.example.finalbe.domains.common.enumdir.Role;
import org.example.finalbe.domains.common.exception.AccessDeniedException;
import org.example.finalbe.domains.common.exception.DuplicateException;
import org.example.finalbe.domains.common.exception.EntityNotFoundException;
import org.example.finalbe.domains.companydatacenter.domain.CompanyDataCenter;
import org.example.finalbe.domains.companydatacenter.repository.CompanyDataCenterRepository;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.dto.*;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.history.service.DataCenterHistoryRecorder;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataCenterService {

    private final DataCenterRepository dataCenterRepository;
    private final MemberRepository memberRepository;
    private final CompanyDataCenterRepository companyDataCenterRepository;
    private final DataCenterHistoryRecorder dataCenterHistoryRecorder;
    /**
     * 현재 로그인한 사용자 조회
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        String userId = authentication.getName();

        if (userId == null || userId.equals("anonymousUser")) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }

        try {
            return memberRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new EntityNotFoundException("사용자", Long.parseLong(userId)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 쓰기 권한 확인 (ADMIN, OPERATOR만 가능)
     */
    private void validateWritePermission(Member member) {
        if (member.getRole() != Role.ADMIN && member.getRole() != Role.OPERATOR) {
            throw new AccessDeniedException("관리자 또는 운영자만 수정할 수 있습니다.");
        }
    }

    /**
     * CompanyDataCenter 매핑 테이블로 접근 권한 확인
     */
    private void validateDataCenterAccess(Member member, Long dataCenterId) {
        if (dataCenterId == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        if (member.getRole() == Role.ADMIN) {
            return; // ADMIN은 모든 전산실 접근 가능
        }

        // CompanyDataCenter 매핑 테이블에서 접근 권한 확인
        boolean hasAccess = companyDataCenterRepository.existsByCompanyIdAndDataCenterId(
                member.getCompany().getId(),
                dataCenterId
        );

        if (!hasAccess) {
            throw new AccessDeniedException("해당 전산실에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * CompanyDataCenter 매핑 테이블로 접근 가능한 전산실 목록 조회
     */
    public List<DataCenterListResponse> getAccessibleDataCenters() {
        Member currentMember = getCurrentMember();
        log.info("Fetching accessible data centers for user: {} (role: {}, company: {})",
                currentMember.getId(), currentMember.getRole(), currentMember.getCompany().getId());

        List<DataCenter> dataCenters;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 모든 전산실 조회
            dataCenters = dataCenterRepository.findByDelYn(DelYN.N);
            log.info("Admin user - returning all {} data centers", dataCenters.size());
        } else {
            // 일반 사용자: CompanyDataCenter 매핑을 통해 접근 가능한 전산실만 조회
            List<CompanyDataCenter> mappings = companyDataCenterRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            dataCenters = mappings.stream()
                    .map(CompanyDataCenter::getDataCenter)
                    .collect(Collectors.toList());

            log.info("Non-admin user - returning {} accessible data centers", dataCenters.size());
        }

        return dataCenters.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전산실 상세 조회
     */
    public DataCenterDetailResponse getDataCenterById(Long id) {
        Member currentMember = getCurrentMember();
        log.info("Fetching data center by id: {} for user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        // CompanyDataCenter 매핑으로 접근 권한 확인
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        return DataCenterDetailResponse.from(dataCenter);
    }

    /**
     * 전산실 생성 + CompanyDataCenter 매핑 자동 생성 + 히스토리 기록
     */
    @Transactional
    public DataCenterDetailResponse createDataCenter(DataCenterCreateRequest request,
                                                     HttpServletRequest httpRequest) {
        Member currentMember = getCurrentMember();
        log.info("Creating data center with code: {} by user: {} (role: {}, company: {})",
                request.code(), currentMember.getId(), currentMember.getRole(),
                currentMember.getCompany().getId());

        validateWritePermission(currentMember);

        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new IllegalArgumentException("전산실 이름을 입력해주세요.");
        }
        if (request.code() == null || request.code().trim().isEmpty()) {
            throw new IllegalArgumentException("전산실 코드를 입력해주세요.");
        }
        if (request.managerId() == null) {
            throw new IllegalArgumentException("담당자를 지정해주세요.");
        }

        if (dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
            throw new DuplicateException("전산실 코드", request.code());
        }

        Member manager = memberRepository.findById(request.managerId())
                .orElseThrow(() -> new EntityNotFoundException("담당자", request.managerId()));

        // 전산실 생성
        DataCenter dataCenter = request.toEntity(manager);
        DataCenter savedDataCenter = dataCenterRepository.save(dataCenter);

        // CompanyDataCenter 매핑 자동 생성
        CompanyDataCenter mapping = CompanyDataCenter.builder()
                .company(currentMember.getCompany())
                .dataCenter(savedDataCenter)
                .description("전산실 생성 시 자동 매핑")
                .grantedBy(currentMember.getUserName())
                .build();
        companyDataCenterRepository.save(mapping);

        // 히스토리 기록
        String ipAddress = getClientIp(httpRequest);
        dataCenterHistoryRecorder.recordCreate(savedDataCenter, currentMember, ipAddress);

        log.info("Data center created successfully with id: {}, automatically mapped to company: {}",
                savedDataCenter.getId(), currentMember.getCompany().getId());

        return DataCenterDetailResponse.from(savedDataCenter);
    }

    /**
     * 전산실 정보 수정 + 히스토리 기록
     */
    @Transactional
    public DataCenterDetailResponse updateDataCenter(Long id,
                                                     DataCenterUpdateRequest request,
                                                     HttpServletRequest httpRequest) {
        Member currentMember = getCurrentMember();
        log.info("Updating data center with id: {} by user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        validateWritePermission(currentMember);
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        // 변경 전 복사
        DataCenter oldDataCenter = copyDataCenter(dataCenter);

        if (request.code() != null
                && !request.code().trim().isEmpty()
                && !request.code().equals(dataCenter.getCode())) {
            if (dataCenterRepository.existsByCodeAndDelYn(request.code(), DelYN.N)) {
                throw new DuplicateException("전산실 코드", request.code());
            }
        }

        Member manager = null;
        if (request.managerId() != null) {
            manager = memberRepository.findById(request.managerId())
                    .orElseThrow(() -> new EntityNotFoundException("담당자", request.managerId()));
        }

        dataCenter.updateInfo(
                request.name(),
                request.code(),
                request.location(),
                request.floor(),
                request.rows(),
                request.columns(),
                request.status(),
                request.description(),
                request.totalArea(),
                request.totalPowerCapacity(),
                request.totalCoolingCapacity(),
                request.maxRackCount(),
                request.temperatureMin(),
                request.temperatureMax(),
                request.humidityMin(),
                request.humidityMax(),
                manager
        );

        // 히스토리 기록
        String ipAddress = getClientIp(httpRequest);
        String reason = request.description();
        dataCenterHistoryRecorder.recordUpdate(oldDataCenter, dataCenter, currentMember, reason, ipAddress);

        log.info("Data center updated successfully with id: {}", id);

        return DataCenterDetailResponse.from(dataCenter);
    }

    /**
     * 전산실 삭제 (Soft Delete) + 히스토리 기록
     */
    @Transactional
    public void deleteDataCenter(Long id,
                                 String reason,
                                 HttpServletRequest httpRequest) {
        Member currentMember = getCurrentMember();
        log.info("Deleting data center with id: {} by user: {} (role: {})",
                id, currentMember.getId(), currentMember.getRole());

        if (id == null) {
            throw new IllegalArgumentException("전산실 ID를 입력해주세요.");
        }

        validateWritePermission(currentMember);
        validateDataCenterAccess(currentMember, id);

        DataCenter dataCenter = dataCenterRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("전산실", id));

        // 삭제 전에 히스토리 기록
        String ipAddress = getClientIp(httpRequest);
        dataCenterHistoryRecorder.recordDelete(dataCenter, currentMember, reason, ipAddress);

        // 소프트 삭제
        dataCenter.softDelete();

        log.info("Data center soft deleted successfully with id: {}", id);
    }

    /**
     * CompanyDataCenter 기반 전산실 이름 검색
     */
    public List<DataCenterListResponse> searchDataCentersByName(String name) {
        Member currentMember = getCurrentMember();
        log.info("Searching data centers by name: {} for user: {} (role: {})",
                name, currentMember.getId(), currentMember.getRole());

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<DataCenter> searchResults;

        if (currentMember.getRole() == Role.ADMIN) {
            // ADMIN은 전체 검색
            searchResults = dataCenterRepository.searchByName(name);
            log.info("Admin user - searched all data centers, found: {}", searchResults.size());
        } else {
            // 일반 사용자: 접근 가능한 전산실 중에서만 검색
            List<CompanyDataCenter> mappings = companyDataCenterRepository
                    .findByCompanyId(currentMember.getCompany().getId());

            searchResults = mappings.stream()
                    .map(CompanyDataCenter::getDataCenter)
                    .filter(dc -> dc.getName().contains(name))
                    .collect(Collectors.toList());

            log.info("Non-admin user - searched accessible data centers, found: {}", searchResults.size());
        }

        return searchResults.stream()
                .map(DataCenterListResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 클라이언트 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * DataCenter Deep Copy (변경 전 상태 저장용)
     */
    private DataCenter copyDataCenter(DataCenter original) {
        return DataCenter.builder()
                .id(original.getId())
                .name(original.getName())
                .code(original.getCode())
                .location(original.getLocation())
                .floor(original.getFloor())
                .rows(original.getRows())
                .columns(original.getColumns())
                .status(original.getStatus())
                .description(original.getDescription())
                .totalArea(original.getTotalArea())
                .totalPowerCapacity(original.getTotalPowerCapacity())
                .totalCoolingCapacity(original.getTotalCoolingCapacity())
                .maxRackCount(original.getMaxRackCount())
                .currentRackCount(original.getCurrentRackCount())
                .temperatureMin(original.getTemperatureMin())
                .temperatureMax(original.getTemperatureMax())
                .humidityMin(original.getHumidityMin())
                .humidityMax(original.getHumidityMax())
                .manager(original.getManager())
                .build();
    }
}