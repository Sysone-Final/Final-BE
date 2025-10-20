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

            // ========== Sheet 1: 랙 데이터 입력 ==========
            Sheet dataSheet = workbook.createSheet("랙 데이터 입력");

            // 스타일 생성
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle exampleStyle = createExampleStyle(workbook);

            // 헤더 행 (첫 번째 행)
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

            // 예시 데이터 행 (두 번째 행)
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

            // ========== Sheet 2: 참조데이터 ==========
            Sheet refSheet = workbook.createSheet("참조데이터");
            createReferenceData(workbook, refSheet, headerStyle);

            // ========== Sheet 3: 작성가이드 ==========
            Sheet guideSheet = workbook.createSheet("작성가이드");
            createGuideSheet(workbook, guideSheet, headerStyle);

            // Excel 파일을 바이트 배열로 변환
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

        // 도어방향
        createReferenceColumn(sheet, headerStyle, colNum++, "도어방향",
                "FRONT", "REAR", "BOTH", "NONE");

        // 존방향
        createReferenceColumn(sheet, headerStyle, colNum++, "존방향",
                "NORTH", "SOUTH", "EAST", "WEST");

        // 상태
        createReferenceColumn(sheet, headerStyle, colNum++, "상태",
                "ACTIVE", "INACTIVE", "MAINTENANCE", "RETIRED");

        // 랙타입
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
                "   - 랙 이름은 전산실 내에서 중복될 수 없습니다",
                "   - 총 유닛은 1~48 사이의 숫자만 입력 가능합니다",
                "   - 담당자ID는 시스템에 등록된 사용자여야 합니다",
                "",
                "5. 필드 설명:",
                "   - 랙이름*: 랙의 고유 이름 (예: RACK-A01)",
                "   - 그룹번호: 랙이 속한 그룹 (예: A-Zone)",
                "   - 위치*: 전산실 내 물리적 위치 (예: Row 1 Col 1)",
                "   - 총유닛*: 랙의 총 유닛 수 (일반적으로 42 또는 48)",
                "   - 도어방향*: FRONT(앞), REAR(뒤), BOTH(양쪽), NONE(없음)",
                "   - 존방향*: NORTH(북), SOUTH(남), EAST(동), WEST(서)",
                "   - 상태*: ACTIVE(활성), INACTIVE(비활성), MAINTENANCE(점검중), RETIRED(폐기)",
                "   - 랙타입*: STANDARD(표준), WALL_MOUNT(벽걸이), OPEN_FRAME(오픈), CABINET(캐비닛)"
        };

        for (int i = 0; i < guides.length; i++) {
            Row row = sheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(guides[i]);
            if (i == 0) {
                cell.setCellStyle(headerStyle);
            }
        }

        sheet.setColumnWidth(0, 20000);
    }

    /**
     * 랙 목록 Excel 내보내기
     */
    public byte[] exportRacksToExcel(Long dataCenterId) {
        log.info("Exporting racks to Excel for datacenter: {}", dataCenterId);

        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        List<Rack> racks = rackRepository.findByDatacenterIdAndDelYn(dataCenterId, DelYN.N);
        log.info("Found {} racks to export", racks.size());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("랙 목록");
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 헤더 행
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "랙ID", "랙이름", "그룹번호", "위치", "총유닛", "사용유닛", "가용유닛", "사용률(%)",
                    "도어방향", "존방향", "부서", "최대전력(W)", "현재전력(W)", "전력사용률(%)",
                    "최대무게(kg)", "현재무게(kg)", "제조사", "시리얼번호", "상태", "랙타입", "담당자ID"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // 데이터 행
            int rowNum = 1;
            for (Rack rack : racks) {
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;

                row.createCell(colNum++).setCellValue(rack.getId());
                row.createCell(colNum++).setCellValue(rack.getRackName());
                row.createCell(colNum++).setCellValue(rack.getGroupNumber());
                row.createCell(colNum++).setCellValue(rack.getRackLocation());
                row.createCell(colNum++).setCellValue(rack.getTotalUnits());
                row.createCell(colNum++).setCellValue(rack.getUsedUnits());
                row.createCell(colNum++).setCellValue(rack.getAvailableUnits());
                row.createCell(colNum++).setCellValue(rack.getUsageRate() != null ? rack.getUsageRate().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(rack.getDoorDirection() != null ? rack.getDoorDirection().name() : "");
                row.createCell(colNum++).setCellValue(rack.getZoneDirection() != null ? rack.getZoneDirection().name() : "");
                row.createCell(colNum++).setCellValue(rack.getDepartment());
                row.createCell(colNum++).setCellValue(rack.getMaxPowerCapacity() != null ? rack.getMaxPowerCapacity().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(rack.getCurrentPowerUsage() != null ? rack.getCurrentPowerUsage().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(rack.getPowerUsageRate() != null ? rack.getPowerUsageRate().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(rack.getMaxWeightCapacity() != null ? rack.getMaxWeightCapacity().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(rack.getCurrentWeight() != null ? rack.getCurrentWeight().doubleValue() : 0.0);
                row.createCell(colNum++).setCellValue(rack.getManufacturer());
                row.createCell(colNum++).setCellValue(rack.getSerialNumber());
                row.createCell(colNum++).setCellValue(rack.getStatus() != null ? rack.getStatus().name() : "");
                row.createCell(colNum++).setCellValue(rack.getRackType() != null ? rack.getRackType().name() : "");
                row.createCell(colNum++).setCellValue(rack.getManagerId());
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export racks to Excel", e);
            throw new RuntimeException("Excel 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 일괄 등록 미리보기
     */
    public RackBulkUploadPreviewResponse previewBulkUpload(MultipartFile file, Long dataCenterId) {
        log.info("Previewing bulk upload for datacenter: {}", dataCenterId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일을 업로드해주세요.");
        }
        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        // 전산실 존재 확인
        DataCenter dataCenter = dataCenterRepository.findById(dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전산실입니다."));

        List<RackBulkUploadPreviewResponse.PreviewRow> previewData = new ArrayList<>();
        List<RackBulkUploadPreviewResponse.ValidationError> errors = new ArrayList<>();
        int validRows = 0;
        int invalidRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("랙 데이터 입력");

            if (sheet == null) {
                throw new IllegalArgumentException("'랙 데이터 입력' 시트를 찾을 수 없습니다.");
            }

            // ✅ 디버깅 로그 추가
            log.info("Sheet name: {}", sheet.getSheetName());
            log.info("Physical number of rows: {}", sheet.getPhysicalNumberOfRows());
            log.info("Last row num: {}", sheet.getLastRowNum());

            int totalRows = 0; // 실제 데이터 행 수

            // 데이터 행 읽기 (3행부터 시작, 인덱스 2)
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                // ✅ 각 행 확인 로그
                log.info("Checking row {}: {}", i, row);

                if (row == null) {
                    log.warn("Row {} is null, skipping", i);
                    continue;
                }

                if (isRowEmpty(row)) {
                    log.warn("Row {} is empty, skipping", i);
                    continue;
                }

                totalRows++; // 실제 데이터 행 증가

                int rowNumber = i + 1;

                // 셀 데이터 읽기
                String rackName = getCellValueAsString(row.getCell(0));
                String groupNumber = getCellValueAsString(row.getCell(1));
                String rackLocation = getCellValueAsString(row.getCell(2));
                Integer totalUnits = getCellValueAsInteger(row.getCell(3));
                String doorDirection = getCellValueAsString(row.getCell(4));
                String zoneDirection = getCellValueAsString(row.getCell(5));
                String status = getCellValueAsString(row.getCell(14));

                // ✅ 읽은 데이터 로그
                log.info("Row {}: rackName={}, groupNumber={}, location={}, units={}",
                        rowNumber, rackName, groupNumber, rackLocation, totalUnits);

                // 검증
                List<String> rowErrors = new ArrayList<>();

                // 필수 항목 검증
                if (rackName == null || rackName.trim().isEmpty()) {
                    rowErrors.add("랙 이름은 필수입니다");
                    errors.add(RackBulkUploadPreviewResponse.ValidationError.builder()
                            .rowNumber(rowNumber)
                            .field("rackName")
                            .message("랙 이름은 필수입니다")
                            .build());
                }

                if (rackLocation == null || rackLocation.trim().isEmpty()) {
                    rowErrors.add("위치는 필수입니다");
                    errors.add(RackBulkUploadPreviewResponse.ValidationError.builder()
                            .rowNumber(rowNumber)
                            .field("rackLocation")
                            .message("위치는 필수입니다")
                            .build());
                }

                if (totalUnits == null) {
                    rowErrors.add("총 유닛은 필수입니다");
                    errors.add(RackBulkUploadPreviewResponse.ValidationError.builder()
                            .rowNumber(rowNumber)
                            .field("totalUnits")
                            .message("총 유닛은 필수입니다")
                            .build());
                } else if (totalUnits < 1 || totalUnits > 48) {
                    rowErrors.add("총 유닛은 1~48 사이여야 합니다");
                    errors.add(RackBulkUploadPreviewResponse.ValidationError.builder()
                            .rowNumber(rowNumber)
                            .field("totalUnits")
                            .message("총 유닛은 1~48 사이여야 합니다")
                            .build());
                }

                // 중복 검증
                if (rackName != null && rackRepository.existsByRackNameAndDatacenterIdAndDelYn(
                        rackName, dataCenterId, DelYN.N)) {
                    rowErrors.add("이미 존재하는 랙 이름입니다");
                    errors.add(RackBulkUploadPreviewResponse.ValidationError.builder()
                            .rowNumber(rowNumber)
                            .field("rackName")
                            .message("이미 존재하는 랙 이름입니다")
                            .build());
                }

                // Enum 검증
                if (doorDirection != null && !doorDirection.trim().isEmpty() && !isValidEnum(DoorDirection.class, doorDirection)) {
                    rowErrors.add("유효하지 않은 도어방향입니다");
                    errors.add(RackBulkUploadPreviewResponse.ValidationError.builder()
                            .rowNumber(rowNumber)
                            .field("doorDirection")
                            .message("유효하지 않은 도어방향입니다 (FRONT/REAR/BOTH/NONE)")
                            .build());
                }

                if (zoneDirection != null && !zoneDirection.trim().isEmpty() && !isValidEnum(ZoneDirection.class, zoneDirection)) {
                    rowErrors.add("유효하지 않은 존방향입니다");
                    errors.add(RackBulkUploadPreviewResponse.ValidationError.builder()
                            .rowNumber(rowNumber)
                            .field("zoneDirection")
                            .message("유효하지 않은 존방향입니다 (NORTH/SOUTH/EAST/WEST)")
                            .build());
                }

                if (status != null && !status.trim().isEmpty() && !isValidEnum(RackStatus.class, status)) {
                    rowErrors.add("유효하지 않은 상태입니다");
                    errors.add(RackBulkUploadPreviewResponse.ValidationError.builder()
                            .rowNumber(rowNumber)
                            .field("status")
                            .message("유효하지 않은 상태입니다 (ACTIVE/INACTIVE/MAINTENANCE/RETIRED)")
                            .build());
                }

                // 결과 집계
                boolean isValid = rowErrors.isEmpty();
                if (isValid) {
                    validRows++;
                } else {
                    invalidRows++;
                }

                // 미리보기 데이터 추가
                previewData.add(RackBulkUploadPreviewResponse.PreviewRow.builder()
                        .rowNumber(rowNumber)
                        .rackName(rackName)
                        .groupNumber(groupNumber)
                        .rackLocation(rackLocation)
                        .totalUnits(totalUnits)
                        .status(status)
                        .isValid(isValid)
                        .errorMessage(rowErrors.isEmpty() ? null : String.join(", ", rowErrors))
                        .build());
            }

            log.info("Total rows processed: {}, valid: {}, invalid: {}", totalRows, validRows, invalidRows);

            return RackBulkUploadPreviewResponse.builder()
                    .totalRows(totalRows)
                    .validRows(validRows)
                    .invalidRows(invalidRows)
                    .previewData(previewData)
                    .errors(errors)
                    .build();

        } catch (IOException e) {
            log.error("Failed to parse Excel file", e);
            throw new RuntimeException("Excel 파일 읽기 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 셀 값을 문자열로 읽기
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
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

    private Long getCellValueAsLong(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case NUMERIC:
                // 숫자형 셀
                return (long) cell.getNumericCellValue();

            case STRING:
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid long value: " + value);
                    return null;
                }

            case FORMULA:
                try {
                    // 수식 결과가 숫자일 수 있음
                    return (long) cell.getNumericCellValue();
                } catch (Exception e) {
                    String formulaValue = cell.getCellFormula();
                    try {
                        return Long.parseLong(formulaValue);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                }

            case BOOLEAN:
                return cell.getBooleanCellValue() ? 1L : 0L;

            default:
                return null;
        }
    }


    /**
     * 행이 비어있는지 확인
     */
    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Enum 값 유효성 검증
     */
    private <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        try {
            Enum.valueOf(enumClass, value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 일괄 등록 실행
     */
    @Transactional
    public RackBulkUploadResultResponse executeBulkUpload(MultipartFile file, Long dataCenterId) {
        log.info("Executing bulk upload for datacenter: {}", dataCenterId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일을 업로드해주세요.");
        }
        if (dataCenterId == null || dataCenterId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 전산실 ID입니다.");
        }

        // 전산실 존재 확인
        DataCenter dataCenter = dataCenterRepository.findById(dataCenterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 전산실입니다."));

        List<RackBulkUploadResultResponse.UploadResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("랙 데이터 입력");

            if (sheet == null) {
                throw new IllegalArgumentException("'랙 데이터 입력' 시트를 찾을 수 없습니다.");
            }

            int totalRows = 0;

            // 데이터 행 읽기 (3행부터)
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int rowNumber = i + 1;

                try {
                    // 셀 데이터 읽기
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

                    // 검증 실패 시
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

                    // DB 저장
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
                    // Enum 변환 실패 등
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

    /**
     * 셀 값을 Double로 읽기
     */
    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                return value.isEmpty() ? null : Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse cell as double: {}", cell);
        }

        return null;
    }

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
}