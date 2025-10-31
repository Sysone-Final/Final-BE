package org.example.finalbe.domains.rack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.finalbe.domains.common.enumdir.*;
import org.example.finalbe.domains.datacenter.domain.DataCenter;
import org.example.finalbe.domains.datacenter.repository.DataCenterRepository;
import org.example.finalbe.domains.member.domain.Member;
import org.example.finalbe.domains.member.repository.MemberRepository;
import org.example.finalbe.domains.rack.domain.Rack;
import org.example.finalbe.domains.rack.dto.*;
import org.example.finalbe.domains.rack.repository.RackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 랙 엑셀 관리 서비스
 * 엑셀 템플릿 생성, 일괄 업로드, 데이터 내보내기 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RackExcelService {

    private final RackRepository rackRepository;
    private final DataCenterRepository dataCenterRepository;
    private final MemberRepository memberRepository;

    /**
     * 일괄 등록 템플릿 생성
     */
    public byte[] generateBulkUploadTemplate() {
        log.info("Generating bulk upload template");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 랙 데이터 입력 시트
            Sheet dataSheet = workbook.createSheet("랙 데이터 입력");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle exampleStyle = createExampleStyle(workbook);

            // 헤더 행
            Row headerRow = dataSheet.createRow(0);
            String[] headers = {
                    "랙이름*", "그룹번호", "위치*", "총유닛*", "도어방향*", "존방향*",
                    "너비(mm)", "깊이(mm)", "높이(mm)", "부서", "최대전력(W)", "최대무게(kg)",
                    "제조사", "시리얼번호", "상태*", "랙타입*", "담당자ID*"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                dataSheet.setColumnWidth(i, 4000);
            }

            // 예시 데이터 행
            Row exampleRow = dataSheet.createRow(1);
            String[] examples = {
                    "RACK-A01", "A-Zone", "Row 1 Col 1", "42", "FRONT", "EAST",
                    "600", "1000", "2000", "IT", "5000", "1000",
                    "Dell", "SN001", "ACTIVE", "STANDARD", "admin"
            };

            for (int i = 0; i < examples.length; i++) {
                Cell cell = exampleRow.createCell(i);
                cell.setCellValue(examples[i]);
                cell.setCellStyle(exampleStyle);
            }

            // 참조데이터 시트
            Sheet refSheet = workbook.createSheet("참조데이터");
            createReferenceData(workbook, refSheet, headerStyle);

            // 작성가이드 시트
            Sheet guideSheet = workbook.createSheet("작성가이드");
            createGuideSheet(workbook, guideSheet, headerStyle);

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate template", e);
            throw new RuntimeException("템플릿 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 헤더 스타일 생성
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    /**
     * 예시 데이터 스타일 생성
     */
    private CellStyle createExampleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 참조 데이터 시트 생성
     */
    private void createReferenceData(Workbook workbook, Sheet sheet, CellStyle headerStyle) {
        int colNum = 0;

        createReferenceColumn(sheet, headerStyle, colNum++, "도어방향",
                "FRONT", "REAR", "BOTH", "NONE");
        createReferenceColumn(sheet, headerStyle, colNum++, "존방향",
                "NORTH", "SOUTH", "EAST", "WEST");
        createReferenceColumn(sheet, headerStyle, colNum++, "상태",
                "ACTIVE", "INACTIVE", "MAINTENANCE", "RETIRED");
        createReferenceColumn(sheet, headerStyle, colNum++, "랙타입",
                "STANDARD", "WALL_MOUNT", "OPEN_FRAME", "CABINET");
    }

    /**
     * 참조 컬럼 생성
     */
    private void createReferenceColumn(Sheet sheet, CellStyle headerStyle,
                                       int colNum, String header, String... values) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            headerRow = sheet.createRow(0);
        }

        Cell headerCell = headerRow.createCell(colNum);
        headerCell.setCellValue(header);
        headerCell.setCellStyle(headerStyle);

        for (int i = 0; i < values.length; i++) {
            Row row = sheet.getRow(i + 1);
            if (row == null) {
                row = sheet.createRow(i + 1);
            }
            row.createCell(colNum).setCellValue(values[i]);
        }

        sheet.setColumnWidth(colNum, 4000);
    }

    /**
     * 작성가이드 시트 생성
     */
    private void createGuideSheet(Workbook workbook, Sheet sheet, CellStyle headerStyle) {
        String[] guides = {
                "=== 랙 일괄 등록 가이드 ===",
                "",
                "1. 필수 항목 (*):",
                "   - 랙이름, 위치, 총유닛, 도어방향, 존방향, 상태, 랙타입, 담당자ID",
                "",
                "2. 작성 예시:",
                "   - '랙 데이터 입력' 시트의 2번째 행을 참고하세요",
                "",
                "3. 참조 데이터:",
                "   - '참조데이터' 시트에서 입력 가능한 값을 확인하세요",
                "",
                "4. 주의사항:",
                "   - 담당자ID는 시스템에 등록된 회원 ID를 입력해주세요",
                "   - 랙 이름은 전산실 내에서 중복될 수 없습니다",
                "   - 총유닛은 1~48 사이의 값을 입력해주세요",
                "",
                "5. 도어방향:",
                "   - FRONT: 전면 / REAR: 후면 / BOTH: 양면 / NONE: 도어 없음",
                "",
                "6. 존방향:",
                "   - NORTH: 북 / SOUTH: 남 / EAST: 동 / WEST: 서",
                "",
                "7. 상태:",
                "   - ACTIVE: 활성 / INACTIVE: 비활성 / MAINTENANCE: 점검중 / RETIRED: 폐기",
                "",
                "8. 랙타입:",
                "   - STANDARD: 표준 / WALL_MOUNT: 벽걸이 / OPEN_FRAME: 오픈프레임 / CABINET: 캐비닛"
        };

        for (int i = 0; i < guides.length; i++) {
            Row row = sheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(guides[i]);
        }

        sheet.setColumnWidth(0, 15000);
    }

    /**
     * 랙 데이터 엑셀 내보내기
     */
    public byte[] exportRacksToExcel(Long dataCenterId) {
        log.info("Exporting racks to Excel for datacenter: {}", dataCenterId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("랙 목록");
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "ID", "랙이름", "그룹번호", "위치", "총유닛", "사용유닛", "가용유닛",
                    "사용률(%)", "도어방향", "존방향", "너비(mm)", "깊이(mm)", "높이(mm)",
                    "부서", "최대전력(W)", "현재전력(W)", "전력사용률(%)",
                    "최대무게(kg)", "현재무게(kg)", "제조사", "시리얼번호",
                    "상태", "랙타입", "담당자ID", "생성일시", "수정일시"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 데이터 조회 및 입력
            List<Rack> racks = rackRepository.findByDatacenterIdAndDelYn(dataCenterId, DelYN.N);

            int rowNum = 1;
            for (Rack rack : racks) {
                Row row = sheet.createRow(rowNum++);
                int cellNum = 0;

                row.createCell(cellNum++).setCellValue(rack.getId());
                row.createCell(cellNum++).setCellValue(rack.getRackName());
                row.createCell(cellNum++).setCellValue(rack.getGroupNumber());
                row.createCell(cellNum++).setCellValue(rack.getRackLocation());
                row.createCell(cellNum++).setCellValue(rack.getTotalUnits());
                row.createCell(cellNum++).setCellValue(rack.getUsedUnits());
                row.createCell(cellNum++).setCellValue(rack.getAvailableUnits());
                row.createCell(cellNum++).setCellValue(rack.getUsageRate() != null ? rack.getUsageRate().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getDoorDirection().name());
                row.createCell(cellNum++).setCellValue(rack.getZoneDirection().name());
                row.createCell(cellNum++).setCellValue(rack.getWidth() != null ? rack.getWidth().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getDepth() != null ? rack.getDepth().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getHeight() != null ? rack.getHeight().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getDepartment());
                row.createCell(cellNum++).setCellValue(rack.getMaxPowerCapacity() != null ? rack.getMaxPowerCapacity().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getCurrentPowerUsage() != null ? rack.getCurrentPowerUsage().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getPowerUsageRate() != null ? rack.getPowerUsageRate().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getMaxWeightCapacity() != null ? rack.getMaxWeightCapacity().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getCurrentWeight() != null ? rack.getCurrentWeight().doubleValue() : 0.0);
                row.createCell(cellNum++).setCellValue(rack.getManufacturer());
                row.createCell(cellNum++).setCellValue(rack.getSerialNumber());
                row.createCell(cellNum++).setCellValue(rack.getStatus().name());
                row.createCell(cellNum++).setCellValue(rack.getRackType().name());
                row.createCell(cellNum++).setCellValue(rack.getManagerId());
                row.createCell(cellNum++).setCellValue(rack.getCreatedAt() != null ? rack.getCreatedAt().toString() : "");
                row.createCell(cellNum++).setCellValue(rack.getUpdatedAt() != null ? rack.getUpdatedAt().toString() : "");
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export racks to Excel", e);
            throw new RuntimeException("엑셀 내보내기 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 일괄 업로드 미리보기
     */
    public RackBulkUploadPreviewResponse previewBulkUpload(MultipartFile file, Long dataCenterId) {
        log.info("Previewing bulk upload for datacenter: {}", dataCenterId);

        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new RuntimeException("전산실을 찾을 수 없습니다."));

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("랙 데이터 입력");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            List<RackBulkUploadPreviewResponse.PreviewRow> items = new ArrayList<>();
            int validCount = 0;
            int invalidCount = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                try {
                    String rackName = getCellValueAsString(row.getCell(0));
                    String groupNumber = getCellValueAsString(row.getCell(1));
                    String rackLocation = getCellValueAsString(row.getCell(2));
                    Integer totalUnits = getCellValueAsInteger(row.getCell(3));
                    String doorDirectionStr = getCellValueAsString(row.getCell(4));
                    String zoneDirectionStr = getCellValueAsString(row.getCell(5));
                    BigDecimal width = getCellValueAsBigDecimal(row.getCell(6));
                    BigDecimal depth = getCellValueAsBigDecimal(row.getCell(7));
                    String department = getCellValueAsString(row.getCell(9));
                    BigDecimal maxPowerCapacity = getCellValueAsBigDecimal(row.getCell(10));
                    BigDecimal maxWeightCapacity = getCellValueAsBigDecimal(row.getCell(11));
                    String manufacturer = getCellValueAsString(row.getCell(12));
                    String serialNumber = getCellValueAsString(row.getCell(13));
                    String statusStr = getCellValueAsString(row.getCell(14));
                    String rackTypeStr = getCellValueAsString(row.getCell(15));
                    Long managerId = getCellValueAsLong(row.getCell(16));

                    // 검증
                    List<String> validationErrors = new ArrayList<>();

                    if (rackName == null || rackName.trim().isEmpty()) {
                        validationErrors.add("랙 이름은 필수입니다");
                    } else if (rackRepository.existsByRackNameAndDatacenterIdAndDelYn(rackName, dataCenterId, DelYN.N)) {
                        validationErrors.add("이미 존재하는 랙 이름입니다");
                    }

                    if (rackLocation == null || rackLocation.trim().isEmpty()) {
                        validationErrors.add("위치는 필수입니다");
                    }

                    if (totalUnits == null || totalUnits < 1 || totalUnits > 48) {
                        validationErrors.add("총 유닛은 1~48 사이여야 합니다");
                    }

                    if (!isValidEnum(DoorDirection.class, doorDirectionStr)) {
                        validationErrors.add("유효하지 않은 도어방향입니다");
                    }

                    if (!isValidEnum(ZoneDirection.class, zoneDirectionStr)) {
                        validationErrors.add("유효하지 않은 존방향입니다");
                    }

                    if (!isValidEnum(RackStatus.class, statusStr)) {
                        validationErrors.add("유효하지 않은 상태입니다");
                    }

                    if (!isValidEnum(RackType.class, rackTypeStr)) {
                        validationErrors.add("유효하지 않은 랙타입입니다");
                    }

                    if (managerId == null) {
                        validationErrors.add("담당자ID는 필수입니다");
                    } else if (!memberRepository.existsById(managerId)) {
                        validationErrors.add("존재하지 않는 담당자ID입니다");
                    }

                    boolean isValid = validationErrors.isEmpty();
                    String errorMessage = validationErrors.isEmpty() ? null : String.join(", ", validationErrors);

                    if (isValid) {
                        validCount++;
                    } else {
                        invalidCount++;
                    }

                    items.add(RackBulkUploadPreviewResponse.PreviewRow.builder()
                            .rowNumber(i)
                            .rackName(rackName)
                            .groupNumber(groupNumber)
                            .rackLocation(rackLocation)
                            .totalUnits(totalUnits)
                            .status(statusStr)
                            .isValid(isValid)
                            .errorMessage(errorMessage)
                            .build());

                } catch (Exception e) {
                    invalidCount++;
                    items.add(RackBulkUploadPreviewResponse.PreviewRow.builder()
                            .rowNumber(i)
                            .rackName(getCellValueAsString(row.getCell(0)))
                            .groupNumber(getCellValueAsString(row.getCell(1)))
                            .rackLocation(getCellValueAsString(row.getCell(2)))
                            .totalUnits(null)
                            .status(null)
                            .isValid(false)
                            .errorMessage("데이터 형식 오류: " + e.getMessage())
                            .build());
                }
            }

            return RackBulkUploadPreviewResponse.builder()
                    .totalRows(items.size())
                    .validRows(validCount)
                    .invalidRows(invalidCount)
                    .previewData(items)
                    .errors(null)
                    .build();

        } catch (IOException e) {
            log.error("Failed to preview bulk upload", e);
            throw new RuntimeException("파일 읽기 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 셀 값을 문자열로 읽기
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        double numericValue = cell.getNumericCellValue();
                        if (numericValue == (long) numericValue) {
                            return String.valueOf((long) numericValue);
                        } else {
                            return String.valueOf(numericValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("Failed to read cell value as string: {}", cell, e);
            return null;
        }
    }

    /**
     * 셀 값을 정수로 읽기
     */
    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                return value.isEmpty() ? null : Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse cell as integer: {}", cell);
        }

        return null;
    }

    /**
     * 셀 값을 Long으로 읽기
     */
    private Long getCellValueAsLong(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (long) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                return value.isEmpty() ? null : Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse cell as long: {}", cell);
        }

        return null;
    }

    /**
     * 셀 값을 BigDecimal로 읽기
     */
    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                return value.isEmpty() ? null : new BigDecimal(value);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse cell as BigDecimal: {}", cell);
        }

        return null;
    }

    /**
     * 행이 비어있는지 확인
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Enum 값 유효성 검사
     */
    private <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            Enum.valueOf(enumClass, value.toUpperCase().trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 일괄 업로드 실행
     */
    @Transactional
    public RackBulkUploadResultResponse executeBulkUpload(MultipartFile file, Long dataCenterId) {
        log.info("Executing bulk upload for datacenter: {}", dataCenterId);

        DataCenter dataCenter = dataCenterRepository.findActiveById(dataCenterId)
                .orElseThrow(() -> new RuntimeException("전산실을 찾을 수 없습니다."));

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("랙 데이터 입력");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            List<RackBulkUploadResultResponse.UploadResult> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;
            int totalRows = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int rowNumber = i;

                try {
                    String rackName = getCellValueAsString(row.getCell(0));
                    String groupNumber = getCellValueAsString(row.getCell(1));
                    String rackLocation = getCellValueAsString(row.getCell(2));
                    Integer totalUnits = getCellValueAsInteger(row.getCell(3));
                    String doorDirectionStr = getCellValueAsString(row.getCell(4));
                    String zoneDirectionStr = getCellValueAsString(row.getCell(5));
                    BigDecimal width = getCellValueAsBigDecimal(row.getCell(6));
                    BigDecimal depth = getCellValueAsBigDecimal(row.getCell(7));
                    String department = getCellValueAsString(row.getCell(9));
                    BigDecimal maxPowerCapacity = getCellValueAsBigDecimal(row.getCell(10));
                    BigDecimal maxWeightCapacity = getCellValueAsBigDecimal(row.getCell(11));
                    String manufacturer = getCellValueAsString(row.getCell(12));
                    String serialNumber = getCellValueAsString(row.getCell(13));
                    String statusStr = getCellValueAsString(row.getCell(14));
                    String rackTypeStr = getCellValueAsString(row.getCell(15));
                    Long managerId = getCellValueAsLong(row.getCell(16));

                    // 검증
                    List<String> errors = new ArrayList<>();

                    if (rackName == null || rackName.trim().isEmpty()) {
                        errors.add("랙 이름은 필수입니다");
                    }
                    if (rackLocation == null || rackLocation.trim().isEmpty()) {
                        errors.add("위치는 필수입니다");
                    }
                    if (totalUnits == null || totalUnits < 1 || totalUnits > 48) {
                        errors.add("총 유닛은 1~48 사이여야 합니다");
                    }
                    if (doorDirectionStr == null || doorDirectionStr.trim().isEmpty()) {
                        errors.add("도어방향은 필수입니다");
                    }
                    if (zoneDirectionStr == null || zoneDirectionStr.trim().isEmpty()) {
                        errors.add("존방향은 필수입니다");
                    }
                    if (statusStr == null || statusStr.trim().isEmpty()) {
                        errors.add("상태는 필수입니다");
                    }
                    if (rackTypeStr == null || rackTypeStr.trim().isEmpty()) {
                        errors.add("랙타입은 필수입니다");
                    }
                    if (managerId == null) {
                        errors.add("담당자ID는 필수입니다");
                    }

                    // 중복 체크
                    if (rackName != null && rackRepository.existsByRackNameAndDatacenterIdAndDelYn(
                            rackName, dataCenterId, DelYN.N)) {
                        errors.add("이미 존재하는 랙 이름입니다");
                    }

                    if (!errors.isEmpty()) {
                        failCount++;
                        results.add(RackBulkUploadResultResponse.UploadResult.builder()
                                .rowNumber(rowNumber)
                                .rackName(rackName)
                                .success(false)
                                .message(String.join(", ", errors))
                                .rackId(null)
                                .build());
                        continue;
                    }

                    // Enum 변환
                    DoorDirection doorDirection = DoorDirection.valueOf(doorDirectionStr.toUpperCase());
                    ZoneDirection zoneDirection = ZoneDirection.valueOf(zoneDirectionStr.toUpperCase());
                    RackStatus status = RackStatus.valueOf(statusStr.toUpperCase());
                    RackType rackType = RackType.valueOf(rackTypeStr.toUpperCase());
                    Optional<Member> member = memberRepository.findById(managerId);
                    String memberName = member.isPresent() ? member.get().getName() : "";

                    // Rack 엔티티 생성
                    Rack rack = Rack.builder()
                            .rackName(rackName)
                            .groupNumber(groupNumber)
                            .rackLocation(rackLocation)
                            .totalUnits(totalUnits != null ? totalUnits : 42)
                            .usedUnits(0)
                            .availableUnits(totalUnits != null ? totalUnits : 42)
                            .doorDirection(doorDirection)
                            .zoneDirection(zoneDirection)
                            .width(width)
                            .depth(depth)
                            .department(department)
                            .maxPowerCapacity(maxPowerCapacity)
                            .currentPowerUsage(BigDecimal.ZERO)
                            .measuredPower(BigDecimal.ZERO)
                            .maxWeightCapacity(maxWeightCapacity)
                            .currentWeight(BigDecimal.ZERO)
                            .manufacturer(manufacturer)
                            .serialNumber(serialNumber)
                            .status(status)
                            .rackType(rackType)
                            .managerId(managerId)
                            .datacenter(dataCenter)
                            .createdBy(memberName)
                            .build();

                    Rack savedRack = rackRepository.save(rack);

                    successCount++;
                    results.add(RackBulkUploadResultResponse.UploadResult.builder()
                            .rowNumber(rowNumber)
                            .rackName(rackName)
                            .success(true)
                            .message("등록 성공")
                            .rackId(savedRack.getId())
                            .build());

                    log.info("Rack created successfully: {} (ID: {})", rackName, savedRack.getId());

                } catch (IllegalArgumentException e) {
                    failCount++;
                    results.add(RackBulkUploadResultResponse.UploadResult.builder()
                            .rowNumber(rowNumber)
                            .rackName(getCellValueAsString(row.getCell(0)))
                            .success(false)
                            .message("유효하지 않은 값: " + e.getMessage())
                            .rackId(null)
                            .build());
                    log.error("Failed to create rack at row {}: {}", rowNumber, e.getMessage());

                } catch (Exception e) {
                    failCount++;
                    results.add(RackBulkUploadResultResponse.UploadResult.builder()
                            .rowNumber(rowNumber)
                            .rackName(getCellValueAsString(row.getCell(0)))
                            .success(false)
                            .message("등록 실패: " + e.getMessage())
                            .rackId(null)
                            .build());
                    log.error("Unexpected error at row {}: {}", rowNumber, e.getMessage(), e);
                }
            }

            log.info("Bulk upload completed: total={}, success={}, fail={}", totalRows, successCount, failCount);

            return RackBulkUploadResultResponse.builder()
                    .totalRows(totalRows)
                    .successCount(successCount)
                    .failCount(failCount)
                    .results(results)
                    .build();

        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            throw new RuntimeException("Excel 파일 읽기 중 오류가 발생했습니다.", e);
        }
    }
}